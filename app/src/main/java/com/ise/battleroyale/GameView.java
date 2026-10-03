package com.ise.battleroyale;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class GameView extends View {

    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    Random random = new Random();

    float playerX = 1200;
    float playerY = 1200;
    float hp = 100;

    float zoneX = 1200;
    float zoneY = 1200;
    float zoneRadius = 1000;

    ArrayList<Bot> bots = new ArrayList<>();

    long lastTime;

    public GameView(Context context) {
        super(context);

        for (int i = 0; i < 12; i++) {
            bots.add(new Bot(
                250 + random.nextInt(1900),
                250 + random.nextInt(1900)
            ));
        }

        lastTime = System.currentTimeMillis();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        long now = System.currentTimeMillis();
        float dt = Math.min(0.05f, (now - lastTime) / 1000f);
        lastTime = now;

        update(dt);

        canvas.drawColor(Color.rgb(45, 90, 55));

        // map grid
        p.setColor(Color.rgb(55, 105, 65));
        p.setStrokeWidth(2);

        for (int x = 0; x < getWidth(); x += 80) {
            canvas.drawLine(x, 0, x, getHeight(), p);
        }

        for (int y = 0; y < getHeight(); y += 80) {
            canvas.drawLine(0, y, getWidth(), y, p);
        }

        // safe zone
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(8);
        p.setColor(Color.CYAN);

        canvas.drawCircle(
            getWidth() / 2f,
            getHeight() / 2f,
            Math.min(getWidth(), getHeight()) * 0.42f,
            p
        );

        p.setStyle(Paint.Style.FILL);

        // bots
        for (Bot bot : bots) {
            if (!bot.alive) continue;

            p.setColor(Color.RED);

            canvas.drawCircle(
                bot.x % getWidth(),
                bot.y % getHeight(),
                22,
                p
            );

            p.setColor(Color.WHITE);
            p.setTextSize(12);

            canvas.drawText(
                "BOT",
                bot.x % getWidth() - 15,
                bot.y % getHeight() - 30,
                p
            );
        }

        // player
        p.setColor(Color.BLUE);

        canvas.drawCircle(
            getWidth() / 2f,
            getHeight() / 2f,
            25,
            p
        );

        // HUD
        p.setColor(Color.WHITE);
        p.setTextSize(24);

        canvas.drawText(
            "ISE BATTLE ROYALE",
            25,
            35,
            p
        );

        canvas.drawText(
            "HP: " + (int) hp,
            25,
            70,
            p
        );

        int alive = 1;

        for (Bot bot : bots) {
            if (bot.alive) alive++;
        }

        canvas.drawText(
            "PLAYERS: " + alive,
            25,
            105,
            p
        );

        postInvalidateDelayed(16);
    }

    void update(float dt) {

        zoneRadius -= dt * 2;

        if (zoneRadius < 180) {
            zoneRadius = 180;
        }

        for (Bot bot : bots) {

            if (!bot.alive) continue;

            float dx = playerX - bot.x;
            float dy = playerY - bot.y;

            float distance =
                (float)Math.sqrt(dx * dx + dy * dy);

            if (distance > 150) {

                bot.x += dx / Math.max(distance, 1) * 45 * dt;
                bot.y += dy / Math.max(distance, 1) * 45 * dt;

            } else {

                hp -= 4 * dt;
            }
        }

        if (hp < 0) hp = 0;
    }

    class Bot {

        float x;
        float y;
        boolean alive = true;

        Bot(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN ||
            event.getAction() == MotionEvent.ACTION_MOVE) {

            // player aim/movement direction
            float dx = event.getX() - getWidth() / 2f;
            float dy = event.getY() - getHeight() / 2f;

            float len = (float)Math.sqrt(dx * dx + dy * dy);

            if (len > 20) {

                playerX += dx / len * 8;
                playerY += dy / len * 8;
            }

            return true;
        }

        return true;
    }
}
