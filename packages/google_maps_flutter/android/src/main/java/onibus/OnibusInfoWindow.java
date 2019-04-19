package onibus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONException;
import org.json.JSONObject;

import io.flutter.plugins.googlemaps.R;

public class OnibusInfoWindow implements GoogleMap.InfoWindowAdapter {
    private Context context;

    public OnibusInfoWindow(Context context) {
        this.context = context;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.onibus_info_window, null);
        TextView linha = view.findViewById(R.id.txt_linha);
        TextView carro = view.findViewById(R.id.txt_carro);
        TextView velocidade = view.findViewById(R.id.txt_velocidade);

        try {
            JSONObject jsonObject = new JSONObject(marker.getSnippet());
            linha.setText(jsonObject.getString("l"));
            carro.setText(jsonObject.getString("c"));
            velocidade.setText(jsonObject.getInt("v") + "");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
