package com.gymtrackerpro.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Muscle map used by the exercise screen. */
public class MuscleMapView extends View {
    private final Bitmap map;
    private final String target;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG);

    public MuscleMapView(Context context, String target) {
        super(context);
        this.target = target == null ? "full" : target;
        map = BitmapFactory.decodeResource(getResources(), R.drawable.muscle_map);
        overlay.setColor(Color.argb(115, 216, 137, 255));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (map == null) return;

        float w = getWidth();
        float h = getHeight();
        float scale = Math.min(w / map.getWidth(), h / map.getHeight());
        float dw = map.getWidth() * scale;
        float dh = map.getHeight() * scale;
        float left = (w - dw) / 2f;
        float top = (h - dh) / 2f;
        RectF dst = new RectF(left, top, left + dw, top + dh);
        canvas.drawBitmap(map, null, dst, paint);

        // A soft highlight keeps the whole map visible while indicating the
        // muscle group selected by the exercise.
        String t = target.toLowerCase(java.util.Locale.ITALIAN);
        if (t.contains("chest") || t.contains("petto")) highlight(canvas, dst, .50f, .28f, .28f, .23f);
        else if (t.contains("back") || t.contains("dorso")) highlight(canvas, dst, .50f, .30f, .28f, .25f);
        else if (t.contains("shoulder") || t.contains("spall")) highlight(canvas, dst, .50f, .17f, .34f, .18f);
        else if (t.contains("biceps") || t.contains("bicip")) highlight(canvas, dst, .35f, .36f, .18f, .30f);
        else if (t.contains("triceps") || t.contains("tricip")) highlight(canvas, dst, .65f, .36f, .18f, .30f);
        else if (t.contains("quad") || t.contains("gamb") || t.contains("addutt")) highlight(canvas, dst, .40f, .72f, .24f, .32f);
        else if (t.contains("hamstring") || t.contains("femoral")) highlight(canvas, dst, .60f, .72f, .24f, .32f);
        else highlight(canvas, dst, .50f, .50f, .55f, .72f);
    }

    private void highlight(Canvas c, RectF dst, float cx, float cy, float rw, float rh) {
        float x1 = dst.left + dst.width() * (cx - rw / 2f);
        float y1 = dst.top + dst.height() * (cy - rh / 2f);
        float x2 = dst.left + dst.width() * (cx + rw / 2f);
        float y2 = dst.top + dst.height() * (cy + rh / 2f);
        c.drawOval(new RectF(x1, y1, x2, y2), overlay);
    }
}
