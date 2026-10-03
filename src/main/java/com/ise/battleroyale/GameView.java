package com.ise.battleroyale;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class GameView extends View {
    static final int WORLD=2400, BOTS=12;
    final Random rng=new Random(77);
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    final ArrayList<Bot> bots=new ArrayList<>();
    final ArrayList<Pickup> pickups=new ArrayList<>();
    float px=1200, py=1200, hp=100, armor=35, ammo=45;
    float zoneX=1200, zoneY=1200, zoneR=1050, aimX, aimY;
    boolean firing=false, gameOver=false, won=false;
    long last, fireAt, matchStart;
    float camX,camY;
    boolean moveUp,moveDown,moveLeft,moveRight;

    public GameView(Context c){ super(c); p.setTypeface(Typeface.create("sans",Typeface.BOLD)); init(); }

    void init(){
        bots.clear(); pickups.clear(); gameOver=won=false; hp=100; armor=35; ammo=45;
        px=1200; py=1200; zoneR=1050; matchStart=System.currentTimeMillis();
        for(int i=0;i<BOTS;i++){
            float a=(float)(i*Math.PI*2/BOTS), r=650+rng.nextFloat()*250;
            bots.add(new Bot(1200+(float)Math.cos(a)*r,1200+(float)Math.sin(a)*r));
        }
        for(int i=0;i<32;i++) pickups.add(new Pickup(180+rng.nextFloat()*2040,180+rng.nextFloat()*2040,
                rng.nextBoolean()?0:1));
        last=System.currentTimeMillis();
    }

    float d(float ax,float ay,float bx,float by){return (float)Math.hypot(ax-bx,ay-by);}
    float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=System.currentTimeMillis(); float dt=Math.min(.05f,(now-last)/1000f); last=now;
        update(dt,now);
        camX=clamp(px-getWidth()/2f,0,WORLD-getWidth());
        camY=clamp(py-getHeight()/2f,0,WORLD-getHeight());
        drawWorld(c); drawHud(c);
        postInvalidateDelayed(16);
    }

    void update(float dt,long now){
        if(gameOver) return;
        float sx=0,sy=0;
        if(moveUp)sy-=1;if(moveDown)sy+=1;if(moveLeft)sx-=1;if(moveRight)sx+=1;
        float len=(float)Math.hypot(sx,sy); if(len>0){px+=sx/len*220*dt;py+=sy/len*220*dt;}
        px=clamp(px,35,WORLD-35);py=clamp(py,35,WORLD-35);

        // Zone slowly contracts.
        float elapsed=(now-matchStart)/1000f;
        zoneR=Math.max(180,1050-elapsed*3.8f);
        if(d(px,py,zoneX,zoneY)>zoneR) hp-=8*dt;

        for(Pickup q:pickups) if(!q.used && d(px,py,q.x,q.y)<38){
            q.used=true; if(q.type==0) ammo+=15; else hp=Math.min(100,hp+28);
        }

        for(Bot b:bots) if(b.alive) b.update(dt,now);
        if(firing && now>fireAt) { shoot(); fireAt=now+180; }
        int alive=0; for(Bot b:bots)if(b.alive)alive++;
        if(hp<=0){hp=0;gameOver=true;won=false;}
        else if(alive==0){gameOver=true;won=true;}
    }

    void shoot(){
        if(ammo<=0) return; ammo--;
        Bot hit=null; float best=75;
        float dx=aimX+camX-px, dy=aimY+camY-py, L=(float)Math.hypot(dx,dy);
        if(L<1)return; dx/=L;dy/=L;
        for(Bot b:bots) if(b.alive){
            float ex=b.x-px,ey=b.y-py; float t=ex*dx+ey*dy;
            if(t>0 && t<700){
                float side=Math.abs(ex*dy-ey*dx);
                if(side<35 && side<best){best=side;hit=b;}
            }
        }
        if(hit!=null){hit.hp-=34; if(hit.hp<=0){hit.alive=false; if(rng.nextBoolean()) pickups.add(new Pickup(hit.x,hit.y,0));}}
    }

    void drawWorld(Canvas c){
        c.drawColor(Color.rgb(40,82,52));
        p.setStyle(Paint.Style.FILL);
        for(int x=0;x<WORLD;x+=80)for(int y=0;y<WORLD;y+=80){
            p.setColor(((x/80+y/80)%2==0)?Color.rgb(48,96,58):Color.rgb(44,90,54));
            c.drawRect(x-camX,y-camY,x+78-camX,y+78-camY,p);
        }
        // Generated rocks/trees
        for(int i=0;i<45;i++){
            float x=(i*313)%WORLD,y=(i*577)%WORLD;
            p.setColor(Color.rgb(34,67,43)); c.drawCircle(x-camX,y-camY,22,p);
            p.setColor(Color.rgb(74,120,66)); c.drawCircle(x-camX-5,y-camY-10,17,p);
        }
        // Safe-zone overlay ring
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);p.setColor(Color.CYAN);
        c.drawCircle(zoneX-camX,zoneY-camY,zoneR,p);p.setStyle(Paint.Style.FILL);

        for(Pickup q:pickups)if(!q.used){
            p.setColor(q.type==0?Color.YELLOW:Color.RED);
            c.drawRect(q.x-12-camX,q.y-12-camY,q.x+12-camX,q.y+12-camY,p);
        }
        for(Bot b:bots)if(b.alive)drawActor(c,b.x,b.y,Color.rgb(210,70,70),b.hp);
        drawActor(c,px,py,Color.rgb(70,145,255),hp);
    }

    void drawActor(Canvas c,float x,float y,int col,float health){
        p.setColor(Color.BLACK);c.drawCircle(x-camX,y-camY,25,p);
        p.setColor(col);c.drawCircle(x-camX,y-camY,20,p);
        p.setColor(Color.WHITE);c.drawCircle(x-camX-7,y-camY-5,4,p);
        p.setColor(Color.BLACK);c.drawCircle(x-camX-7,y-camY-5,2,p);
        p.setColor(Color.RED);c.drawRect(x-22-camX,y-34-camY,x+22-camX,y-29-camY,p);
        p.setColor(Color.GREEN);c.drawRect(x-22-camX,y-34-camY,x-22+44*health/100-camX,y-29-camY,p);
    }

    void drawHud(Canvas c){
        p.setStyle(Paint.Style.FILL); p.setColor(0xAA000000);
        c.drawRect(15,15,285,88,p);
        p.setColor(Color.WHITE);p.setTextSize(22);
        c.drawText("HP "+(int)hp+"   ARM "+(int)armor+"   AMMO "+(int)ammo,28,45,p);
        int alive=1;for(Bot b:bots)if(b.alive)alive++;
        c.drawText("SURVIVORS "+alive,28,72,p);

        // virtual controls
        p.setColor(0x66333333);c.drawCircle(105,getHeight()-105,75,p);
        p.setColor(Color.WHITE);p.setTextSize(30);
        c.drawText("▲",91,getHeight()-140,p);c.drawText("▼",91,getHeight()-45,p);
        c.drawText("◀",55,getHeight()-92,p);c.drawText("▶",137,getHeight()-92,p);
        p.setColor(0xAAFFFFFF);c.drawCircle(getWidth()-105,getHeight()-105,72,p);
        p.setColor(Color.RED);p.setTextSize(24);c.drawText("FIRE",getWidth()-133,getHeight()-98,p);

        p.setColor(Color.WHITE);p.setTextSize(18);
        c.drawText("BLUE RING = SAFE ZONE",getWidth()-240,30,p);

        if(gameOver){
            p.setColor(0xDD000000);c.drawRect(0,0,getWidth(),getHeight(),p);
            p.setColor(won?Color.GREEN:Color.RED);p.setTextSize(54);
            String s=won?"VICTORY":"ELIMINATED";
            c.drawText(s,getWidth()/2-p.measureText(s)/2,getHeight()/2,p);
            p.setColor(Color.WHITE);p.setTextSize(22);
            String t="Tap anywhere to restart";
            c.drawText(t,getWidth()/2-p.measureText(t)/2,getHeight()/2+50,p);
        }
    }

    class Bot{
        float x,y,hp=100; boolean alive=true; long nextShot;
        Bot(float a,float b){x=a;y=b;}
        void update(float dt,long now){
            float dist=d(x,y,px,py);
            float tx=px,ty=py;
            if(dist<650){ // chase and shoot
                if(dist>230){x+=(tx-x)/dist*105*dt;y+=(ty-y)/dist*105*dt;}
                if(dist<520 && now>nextShot){
                    nextShot=now+900+rng.nextInt(700);
                    float damage=8;
                    if(armor>0){float a=Math.min(armor,damage*.55f);armor-=a;damage-=a;}
                    hp-=0; GameView.this.hp-=damage;
                }
            } else { // wander toward center
                float cd=d(x,y,zoneX,zoneY);
                if(cd>zoneR*.92f){x+=(zoneX-x)/Math.max(cd,1)*90*dt;y+=(zoneY-y)/Math.max(cd,1)*90*dt;}
            }
            if(d(x,y,zoneX,zoneY)>zoneR)hp-=5*dt;
            if(hp<=0)alive=false;
        }
    }
    class Pickup{float x,y;int type;boolean used;Pickup(float a,float b,int t){x=a;y=b;type=t;}}

    @Override public boolean onTouchEvent(android.view.MotionEvent e){
        float x=e.getX(),y=e.getY();
        if(e.getAction()==MotionEvent.ACTION_DOWN){
            if(gameOver){init();return true;}
            if(x>getWidth()-190 && y>getHeight()-190){firing=true;aimX=x;aimY=y;}
            else if(y>getHeight()-200){
                moveUp=y<getHeight()-125; moveDown=y>getHeight()-75;
                moveLeft=x<145 && y>getHeight()-145; moveRight=x>145 && x<210 && y>getHeight()-145;
                // coarse directional zones
                if(x<210){moveLeft=x<105;moveRight=x>=105;}
            }
        } else if(e.getAction()==MotionEvent.ACTION_MOVE){
            if(firing){aimX=x;aimY=y;}
        } else if(e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL){
            firing=false;moveUp=moveDown=moveLeft=moveRight=false;
        }
        return true;
    }
}
