package onibus;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import io.flutter.plugins.googlemaps.R;

public class OnibusInfoWindow implements GoogleMap.InfoWindowAdapter {
    private Context context;

    public OnibusInfoWindow(Context context) {
        this.context = context;
    }


    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public float convertDpToPixel(float dp){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @return A float value to represent dp equivalent to px value
     */
    public float convertPixelstTokensoDp(float px){
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        try {
            JSONObject jsonObject = new JSONObject(marker.getSnippet());
            Calendar calendar = Calendar.getInstance();
            long dateDiff = calendar.getTimeInMillis() - jsonObject.getLong("D");

            calendar.setTimeInMillis(946692000000L); //2000-01-01 00:00:00
            calendar.add(Calendar.MILLISECOND, (int) dateDiff);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.onibus_info_window, null);
            TextView linha = view.findViewById(R.id.txt_linha);
            TextView carro = view.findViewById(R.id.txt_carro);
            TextView velocidade = view.findViewById(R.id.txt_velocidade);
            TextView time = view.findViewById(R.id.txt_time);
            TextView gpsOff = view.findViewById(R.id.gps_fora_alcance);
            TextView semLinha = view.findViewById(R.id.gps_sem_linha);

            String linhaMarker = jsonObject.getString("l");

            if(!linhaMarker.isEmpty()) {
                linha.setText(linhaMarker);
            } else {
                semLinha.setVisibility(View.VISIBLE);

                //Adiciona um espaço
                gpsOff.setVisibility(View.INVISIBLE);
            }

            carro.setText(jsonObject.getString("c"));
            velocidade.setText(jsonObject.getString("v"));


            if(calendar.get(Calendar.HOUR_OF_DAY) > 0 || calendar.get(Calendar.DAY_OF_MONTH) > 1) {
                gpsOff.setVisibility(View.VISIBLE);
                if(semLinha.getVisibility() == View.GONE) {
                    //Adiciona um "espaço"
                    semLinha.setVisibility(View.INVISIBLE);
                }

                if(calendar.get(Calendar.DAY_OF_MONTH) > 1) {
                    time.setText(formatterDays(calendar), TextView.BufferType.SPANNABLE);

                } else {
                    time.setText(formatterOnlyHours(calendar), TextView.BufferType.SPANNABLE);
                }

            } else if(calendar.get(Calendar.MINUTE) > 0 ) {
                time.setText(formatterMinuteAndSecounds(calendar), TextView.BufferType.SPANNABLE);
            } else {
                time.setText(formatterOnlySecounds(calendar), TextView.BufferType.SPANNABLE);
            }

            return view;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private SpannableString formatterDays(Calendar date) {
//        String strDate = new java.text.SimpleDateFormat("H").format(date.getTime());
//
//        String[] strDateTokens = strDate.split(":");
        List<String> lstTokens = new ArrayList<>();

        String horas = "+24h";
        lstTokens.add(horas); //Plural

        String finalToken = " sem sinal";
        lstTokens.add(finalToken); //Plural

        String finalText = horas + finalToken;
        SpannableString spanString = new SpannableString(finalText);

        int sizePrimary = (int) convertDpToPixel(14);
        int sizeSecundary = (int) convertDpToPixel(10);
        spanString.setSpan(new AbsoluteSizeSpan(sizePrimary), finalText.indexOf(lstTokens.get(0)), finalText.indexOf(lstTokens.get(0)) + lstTokens.get(0).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanString.setSpan(new AbsoluteSizeSpan(sizeSecundary), finalText.indexOf(lstTokens.get(1)), finalText.indexOf(lstTokens.get(1)) + lstTokens.get(1).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanString.setSpan(new ForegroundColorSpan(Color.parseColor("#303030")), finalText.indexOf(lstTokens.get(1)), finalText.indexOf(lstTokens.get(1)) + lstTokens.get(1).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spanString;
    }

    private SpannableString formatterOnlySecounds(Calendar date) {
        String strDate = new java.text.SimpleDateFormat("s").format(date.getTime());

        String[] strDateTokens = strDate.split(":");
        List<String> lstTokens = new ArrayList<>(Arrays.asList(strDateTokens));

        String finalToken = " segundo" + (date.get(Calendar.SECOND) > 1 ? "s" : "") + " atrás";
        lstTokens.add(finalToken); //Plural

        String finalText = strDate + finalToken;
        SpannableString spanString = new SpannableString(finalText);

        int sizePrimary = (int) convertDpToPixel(20);
        int sizeSecondary = (int) convertDpToPixel(13);
        int sizeTerciary = (int) convertDpToPixel(10);

        //Segundo
        spanString.setSpan(new AbsoluteSizeSpan(sizePrimary), finalText.indexOf(lstTokens.get(0)), finalText.indexOf(lstTokens.get(0)) + lstTokens.get(0).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanString.setSpan(new AbsoluteSizeSpan(sizeTerciary), finalText.indexOf(lstTokens.get(1)), finalText.indexOf(lstTokens.get(1)) + lstTokens.get(1).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanString.setSpan(new ForegroundColorSpan(Color.parseColor("#303030")), finalText.indexOf(lstTokens.get(1)), finalText.indexOf(lstTokens.get(1)) + lstTokens.get(1).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanString;
    }

    private SpannableString formatterMinuteAndSecounds(Calendar date) {
        String strDate = new java.text.SimpleDateFormat("m:ss").format(date.getTime());

        String[] strDateTokens = strDate.split(":");
        List<String> lstTokens = new ArrayList<>(Arrays.asList(strDateTokens));
        String finalToken = " atrás";
        lstTokens.add(finalToken);

        String finalText = strDate + finalToken;
        SpannableString spanString = new SpannableString(finalText);

        int sizePrimary = (int) convertDpToPixel(20);
        int sizeSecondary = (int) convertDpToPixel(13);
        int sizeTerciary = (int) convertDpToPixel(10);

        //Minuto
        spanString.setSpan(new AbsoluteSizeSpan(sizePrimary), finalText.indexOf(lstTokens.get(0)), finalText.indexOf(lstTokens.get(0)) + lstTokens.get(0).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanString.setSpan(new AbsoluteSizeSpan(sizeSecondary), finalText.indexOf(lstTokens.get(0)) + lstTokens.get(0).length(), finalText.indexOf(lstTokens.get(0)) + lstTokens.get(0).length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        //Segundo
        spanString.setSpan(new AbsoluteSizeSpan(sizePrimary), finalText.indexOf(lstTokens.get(1)), finalText.indexOf(lstTokens.get(1)) + lstTokens.get(1).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanString.setSpan(new AbsoluteSizeSpan(sizeTerciary), finalText.indexOf(lstTokens.get(2)), finalText.indexOf(lstTokens.get(2)) + lstTokens.get(2).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanString.setSpan(new ForegroundColorSpan(Color.parseColor("#303030")), finalText.indexOf(lstTokens.get(2)), finalText.indexOf(lstTokens.get(2)) + lstTokens.get(2).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spanString;
    }

    private SpannableString formatterOnlyHours(Calendar date) {
        String strDate = new java.text.SimpleDateFormat("H").format(date.getTime());

        String[] strDateTokens = strDate.split(":");
        List<String> lstTokens = new ArrayList<>(Arrays.asList(strDateTokens));

        String finalToken = " hora" + (date.get(Calendar.HOUR_OF_DAY) > 1 ? "s" : "") + " atrás";
        lstTokens.add(finalToken); //Plural

        String finalText = strDate + finalToken;
        SpannableString spanString = new SpannableString(finalText);

        int sizePrimary = (int) convertDpToPixel(20);
        int sizeSecondary = (int) convertDpToPixel(13);
        int sizeTerciary = (int) convertDpToPixel(10);

        //Hora
        spanString.setSpan(new AbsoluteSizeSpan(sizePrimary), finalText.indexOf(lstTokens.get(0)), finalText.indexOf(lstTokens.get(0)) + lstTokens.get(0).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanString.setSpan(new AbsoluteSizeSpan(sizeTerciary), finalText.indexOf(lstTokens.get(1)), finalText.indexOf(lstTokens.get(1)) + lstTokens.get(1).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanString.setSpan(new ForegroundColorSpan(Color.parseColor("#303030")), finalText.indexOf(lstTokens.get(1)), finalText.indexOf(lstTokens.get(1)) + lstTokens.get(1).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spanString;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
