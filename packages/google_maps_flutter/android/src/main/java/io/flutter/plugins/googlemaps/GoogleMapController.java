// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.CREATED;
import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.DESTROYED;
import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.PAUSED;
import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.RESUMED;
import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.STARTED;
import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.STOPPED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.platform.PlatformView;
import onibus.OnibusInfoWindow;
import onibus.OnibusMarkerClick;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Controller of a single GoogleMaps MapView instance. */
final class GoogleMapController
    implements Application.ActivityLifecycleCallbacks,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnPolygonClickListener,
        GoogleMap.OnPolylineClickListener,
        GoogleMap.OnCircleClickListener,
        GoogleMapOptionsSink,
        MethodChannel.MethodCallHandler,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        PlatformView {

  private static final String TAG = "GoogleMapController";
  private final int id;
  private final AtomicInteger activityState;
  private final MethodChannel methodChannel;
  private final PluginRegistry.Registrar registrar;
  private final MapView mapView;
  private OnibusMarkerClick onibusMarkerClick;
  private GoogleMap googleMap;
  private boolean trackCameraPosition = false;
  private boolean myLocationEnabled = false;
  private boolean trafficEnabled = false;
  private boolean myLocationButtonEnabled = false;
  private boolean disposed = false;
  private final float density;
  private MethodChannel.Result mapReadyResult;
  private final int registrarActivityHashCode;
  private final Context context;
  private final MarkersController markersController;
  private final PolygonsController polygonsController;
  private final PolylinesController polylinesController;
  private final CirclesController circlesController;
  private List<Object> initialMarkers;
  private List<Object> initialPolygons;
  private List<Object> initialPolylines;
  private List<Object> initialCircles;

  GoogleMapController(
      int id,
      Context context,
      AtomicInteger activityState,
      PluginRegistry.Registrar registrar,
      GoogleMapOptions options) {
    this.id = id;
    this.context = context;
    this.activityState = activityState;
    this.registrar = registrar;
    this.mapView = new MapView(context, options);
    if (needDisableHardwareAcceleration()) {
      this.mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    this.density = context.getResources().getDisplayMetrics().density;
    methodChannel =
        new MethodChannel(registrar.messenger(), "plugins.flutter.io/google_maps_" + id);
    methodChannel.setMethodCallHandler(this);
    this.registrarActivityHashCode = registrar.activity().hashCode();
    this.onibusMarkerClick = new OnibusMarkerClick();
    this.markersController = new MarkersController(methodChannel);
    this.polygonsController = new PolygonsController(methodChannel);
    this.polylinesController = new PolylinesController(methodChannel);
    this.circlesController = new CirclesController(methodChannel);
  }

  private boolean needDisableHardwareAcceleration() {
    List<Devices> devices = Arrays.asList(
            new Devices("dreamlte", Build.VERSION_CODES.O),
            new Devices("sanders_nt", Build.VERSION_CODES.O_MR1),
            new Devices("j4primelte", Build.VERSION_CODES.O_MR1), //2
            new Devices("ASUS_X00QD", Build.VERSION_CODES.P),
            new Devices("tulip", Build.VERSION_CODES.P),
            new Devices("tulip", Build.VERSION_CODES.O_MR1),
            new Devices("j7elte", Build.VERSION_CODES.M), //Dispositivo desligando
            new Devices("titan_udstv", Build.VERSION_CODES.M), // Dispositivo desligando
            new Devices("titan_umtsds", Build.VERSION_CODES.M), // Comportamento estranho
            new Devices("beryllium", Build.VERSION_CODES.P),
            new Devices("dipper", Build.VERSION_CODES.P),
            new Devices("on5xelte", Build.VERSION_CODES.O),
            new Devices("rosy", Build.VERSION_CODES.O_MR1),
            new Devices("potter_nt", Build.VERSION_CODES.O_MR1),
            new Devices("cedric", Build.VERSION_CODES.O_MR1),
            new Devices("j4corelte", Build.VERSION_CODES.O_MR1),
            new Devices("pettyl", Build.VERSION_CODES.O_MR1),
            new Devices("athene_f", Build.VERSION_CODES.O_MR1),
            new Devices("j2corelte", Build.VERSION_CODES.O_MR1),
            new Devices("ASUS_X018_4", Build.VERSION_CODES.N),
            new Devices("montana", Build.VERSION_CODES.N), //Nao sei qual problema, pode ter sido bug de versoes anteriores
            new Devices("lake_n", Build.VERSION_CODES.P));

      Devices currentDevice = new Devices(Build.DEVICE, Build.VERSION.SDK_INT);
      currentDevice.setProduct(Build.PRODUCT);

      for (Devices _device: devices) {
          if(_device.equals(currentDevice))
              return true;
      }

      return false;
  }

  private String defaultMapaStyle() {
    String result = "[]";
    try {

      StringBuilder sb = new StringBuilder();
      AssetManager assetManager = registrar.context().getAssets();
      String key = registrar.lookupKeyForAsset("assets/mapStyle/light2.json");
      InputStream is =assetManager.open(key);
      BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      String str;
      while ((str = br.readLine()) != null) {
        sb.append(str);
      }
      result = sb.toString();
      br.close();

    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  @Override
  public View getView() {
    return mapView;
  }

  void init() {
    switch (activityState.get()) {
      case STOPPED:
        mapView.onCreate(null);
        mapView.onStart();
        mapView.onResume();
        mapView.onPause();
        mapView.onStop();
        break;
      case PAUSED:
        mapView.onCreate(null);
        mapView.onStart();
        mapView.onResume();
        mapView.onPause();
        break;
      case RESUMED:
        mapView.onCreate(null);
        mapView.onStart();
        mapView.onResume();
        break;
      case STARTED:
        mapView.onCreate(null);
        mapView.onStart();
        break;
      case CREATED:
        mapView.onCreate(null);
        break;
      case DESTROYED:
        // Nothing to do, the activity has been completely destroyed.
        break;
      default:
        throw new IllegalArgumentException(
            "Cannot interpret " + activityState.get() + " as an activity state");
    }
    registrar.activity().getApplication().registerActivityLifecycleCallbacks(this);
    mapView.getMapAsync(this);
  }

  private void moveCamera(CameraUpdate cameraUpdate) {
    googleMap.moveCamera(cameraUpdate);
  }

  private void animateCamera(CameraUpdate cameraUpdate, int duration) {
    googleMap.animateCamera(cameraUpdate, duration, null);
  }

  private CameraPosition getCameraPosition() {
    return trackCameraPosition ? googleMap.getCameraPosition() : null;
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    this.googleMap = googleMap;
    //googleMap.setOnInfoWindowClickListener(this);
    if (mapReadyResult != null) {
      mapReadyResult.success(null);
      mapReadyResult = null;
    }
    googleMap.setOnCameraMoveStartedListener(this);
    googleMap.setOnCameraMoveListener(this);
    googleMap.setOnCameraIdleListener(this);
    googleMap.setOnMarkerClickListener(this);
    googleMap.setOnPolygonClickListener(this);
    googleMap.setOnPolylineClickListener(this);
    googleMap.setOnCircleClickListener(this);
    googleMap.setOnMapClickListener(this);
    googleMap.setInfoWindowAdapter(new OnibusInfoWindow(this.context));
    googleMap.setOnMapLongClickListener(this);
    updateMyLocationSettings();
    markersController.setGoogleMap(googleMap);
    polygonsController.setGoogleMap(googleMap);
    polylinesController.setGoogleMap(googleMap);
    circlesController.setGoogleMap(googleMap);
    updateInitialMarkers();
    updateInitialPolygons();
    updateInitialPolylines();
    updateInitialCircles();
    googleMap.getUiSettings().setZoomControlsEnabled(false);
    googleMap.getUiSettings().setCompassEnabled(false);
    googleMap.getUiSettings().setIndoorLevelPickerEnabled(false);
    googleMap.getUiSettings().setMapToolbarEnabled(false);

    // COMENTADO DEVIDO A ALTERAÇÃO NO FUNCIONAMENTO DO ESTILO DO MAPA
    // AGORA FUNCIONA VIA CONTROLLER
    // CASO TUDO FUNCIONE MUITO BEM, ISSO PODE SER REMOVIDO
    //if(mapStyle == null || mapStyle.isEmpty()) {
    //  this.mapStyle = defaultMapaStyle();
    //}
    //googleMap.setMapStyle(new MapStyleOptions(mapStyle));

  }

  @Override
  public void onMethodCall(MethodCall call, MethodChannel.Result result) {
    switch (call.method) {
      case "map#waitForMap":
        if (googleMap != null) {
          result.success(null);
          return;
        }
        mapReadyResult = result;
        break;
      case "map#update":
        {
          Convert.interpretGoogleMapOptions(call.argument("options"), this);
          result.success(Convert.cameraPositionToJson(getCameraPosition()));
          break;
        }
      case "map#getVisibleRegion":
        {
          if (googleMap != null) {
            LatLngBounds latLngBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
            result.success(Convert.latlngBoundsToJson(latLngBounds));
          } else {
            result.error(
                "GoogleMap uninitialized",
                "getVisibleRegion called prior to map initialization",
                null);
          }
          break;
        }
      case "camera#move":
        {
          final CameraUpdate cameraUpdate =
              Convert.toCameraUpdate(call.argument("cameraUpdate"), density);
          moveCamera(cameraUpdate);
          result.success(null);
          break;
        }
      case "camera#animate":
        {
          final CameraUpdate cameraUpdate =
              Convert.toCameraUpdate(call.argument("cameraUpdate"), density);
          final int duration = (int) call.argument("duration");
          animateCamera(cameraUpdate, duration);
          result.success(null);
          break;
        }
      case "markers#update":
        {
          Object markersToAdd = call.argument("markersToAdd");
          markersController.addMarkers((List<Object>) markersToAdd);
          Object markersToChange = call.argument("markersToChange");
          markersController.changeMarkers((List<Object>) markersToChange);
          Object markerIdsToRemove = call.argument("markerIdsToRemove");
          markersController.removeMarkers((List<Object>) markerIdsToRemove);
          result.success(null);
          break;
        }
      case "polygons#update":
        {
          Object polygonsToAdd = call.argument("polygonsToAdd");
          polygonsController.addPolygons((List<Object>) polygonsToAdd);
          Object polygonsToChange = call.argument("polygonsToChange");
          polygonsController.changePolygons((List<Object>) polygonsToChange);
          Object polygonIdsToRemove = call.argument("polygonIdsToRemove");
          polygonsController.removePolygons((List<Object>) polygonIdsToRemove);
          result.success(null);
          break;
        }
      case "polylines#update":
        {
          Object polylinesToAdd = call.argument("polylinesToAdd");
          polylinesController.addPolylines((List<Object>) polylinesToAdd);
          Object polylinesToChange = call.argument("polylinesToChange");
          polylinesController.changePolylines((List<Object>) polylinesToChange);
          Object polylineIdsToRemove = call.argument("polylineIdsToRemove");
          polylinesController.removePolylines((List<Object>) polylineIdsToRemove);
          result.success(null);
          break;
        }
      case "circles#update":
        {
          Object circlesToAdd = call.argument("circlesToAdd");
          circlesController.addCircles((List<Object>) circlesToAdd);
          Object circlesToChange = call.argument("circlesToChange");
          circlesController.changeCircles((List<Object>) circlesToChange);
          Object circleIdsToRemove = call.argument("circleIdsToRemove");
          circlesController.removeCircles((List<Object>) circleIdsToRemove);
          result.success(null);
          break;
        }
      case "map#isCompassEnabled":
        {
          result.success(googleMap.getUiSettings().isCompassEnabled());
          break;
        }
      case "map#getMinMaxZoomLevels":
        {
          List<Float> zoomLevels = new ArrayList<>(2);
          zoomLevels.add(googleMap.getMinZoomLevel());
          zoomLevels.add(googleMap.getMaxZoomLevel());
          result.success(zoomLevels);
          break;
        }
      case "map#isZoomGesturesEnabled":
        {
          result.success(googleMap.getUiSettings().isZoomGesturesEnabled());
          break;
        }
      case "map#isScrollGesturesEnabled":
        {
          result.success(googleMap.getUiSettings().isScrollGesturesEnabled());
          break;
        }
      case "map#isTiltGesturesEnabled":
        {
          result.success(googleMap.getUiSettings().isTiltGesturesEnabled());
          break;
        }
      case "map#isRotateGesturesEnabled":
        {
          result.success(googleMap.getUiSettings().isRotateGesturesEnabled());
          break;
        }
      case "map#isMyLocationButtonEnabled":
        {
          result.success(googleMap.getUiSettings().isMyLocationButtonEnabled());
          break;
        }
      case "map#setStyle":
        {
          String mapStyle = (String) call.arguments;
          boolean mapStyleSet;
          if (mapStyle == null) {
            mapStyleSet = googleMap.setMapStyle(null);
          } else {
            mapStyleSet = googleMap.setMapStyle(new MapStyleOptions(mapStyle));
          }
          ArrayList<Object> mapStyleResult = new ArrayList<>(2);
          mapStyleResult.add(mapStyleSet);
          if (!mapStyleSet) {
            mapStyleResult.add(
                "Unable to set the map style. Please check console logs for errors.");
          }
          result.success(mapStyleResult);
          break;
        }
      default:
        result.notImplemented();
    }
  }

  @Override
  public void onMapClick(LatLng latLng) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("position", Convert.latLngToJson(latLng));
    methodChannel.invokeMethod("map#onTap", arguments);
  }

  @Override
  public void onMapLongClick(LatLng latLng) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("position", Convert.latLngToJson(latLng));
    methodChannel.invokeMethod("map#onLongPress", arguments);
  }

  @Override
  public void onCameraMoveStarted(int reason) {
    final Map<String, Object> arguments = new HashMap<>(2);
    boolean isGesture = reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE;
    arguments.put("isGesture", isGesture);
    methodChannel.invokeMethod("camera#onMoveStarted", arguments);
  }

  @Override
  public void onInfoWindowClick(Marker marker) {
    markersController.onInfoWindowTap(marker.getId());
  }

  @Override
  public void onCameraMove() {
    if (!trackCameraPosition) {
      return;
    }
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("position", Convert.cameraPositionToJson(googleMap.getCameraPosition()));
    methodChannel.invokeMethod("camera#onMove", arguments);
  }

  @Override
  public void onCameraIdle() {
    methodChannel.invokeMethod("camera#onIdle", Collections.singletonMap("map", id));
  }

  @Override
  public boolean onMarkerClick(Marker marker) {
    return onibusMarkerClick.onMarkerClick(marker)

    // Comentado para poder customizar o evento de click e não assumir o default do Plugin Flutter
    // return markersController.onMarkerTap(marker.getId());
  }

  @Override
  public void onPolygonClick(Polygon polygon) {
    polygonsController.onPolygonTap(polygon.getId());
  }

  @Override
  public void onPolylineClick(Polyline polyline) {
    polylinesController.onPolylineTap(polyline.getId());
  }

  @Override
  public void onCircleClick(Circle circle) {
    circlesController.onCircleTap(circle.getId());
  }

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    methodChannel.setMethodCallHandler(null);
    mapView.onDestroy();
    registrar.activity().getApplication().unregisterActivityLifecycleCallbacks(this);
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onCreate(savedInstanceState);
  }

  @Override
  public void onActivityStarted(Activity activity) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onStart();
  }

  @Override
  public void onActivityResumed(Activity activity) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }

    onibusMarkerClick.onRemuse();
    mapView.onResume();
  }

  @Override
  public void onActivityPaused(Activity activity) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }

    onibusMarkerClick.onPause();
    mapView.onPause();
  }

  @Override
  public void onActivityStopped(Activity activity) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onStop();
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onDestroy();
  }

  // GoogleMapOptionsSink methods

  @Override
  public void setCameraTargetBounds(LatLngBounds bounds) {
    googleMap.setLatLngBoundsForCameraTarget(bounds);
  }

  @Override
  public void setCompassEnabled(boolean compassEnabled) {
    googleMap.getUiSettings().setCompassEnabled(compassEnabled);
  }

  @Override
  public void setMapType(int mapType) {
    googleMap.setMapType(mapType);
  }

  @Override
  public void setMapStyle(String mapStyle) {
    if (mapStyle == null || this.mapStyle.equals(mapStyle) || mapStyle.isEmpty()) {
      return;
    }
    this.mapStyle = mapStyle;
    if (googleMap != null) {
      googleMap.setMapStyle(new MapStyleOptions(mapStyle));
    }
  }

  @Override
  public void setTrackCameraPosition(boolean trackCameraPosition) {
    this.trackCameraPosition = trackCameraPosition;
  }

  @Override
  public void setRotateGesturesEnabled(boolean rotateGesturesEnabled) {
    googleMap.getUiSettings().setRotateGesturesEnabled(rotateGesturesEnabled);
  }

  @Override
  public void setScrollGesturesEnabled(boolean scrollGesturesEnabled) {
    googleMap.getUiSettings().setScrollGesturesEnabled(scrollGesturesEnabled);
  }

  @Override
  public void setTiltGesturesEnabled(boolean tiltGesturesEnabled) {
    googleMap.getUiSettings().setTiltGesturesEnabled(tiltGesturesEnabled);
  }

  @Override
  public void setMinMaxZoomPreference(Float min, Float max) {
    googleMap.resetMinMaxZoomPreference();
    if (min != null) {
      googleMap.setMinZoomPreference(min);
    }
    if (max != null) {
      googleMap.setMaxZoomPreference(max);
    }
  }

  @Override
  public void setZoomGesturesEnabled(boolean zoomGesturesEnabled) {
    googleMap.getUiSettings().setZoomGesturesEnabled(zoomGesturesEnabled);
  }

  @Override
  public void setMyLocationEnabled(boolean myLocationEnabled) {
    if (this.myLocationEnabled == myLocationEnabled) {
      return;
    }
    this.myLocationEnabled = myLocationEnabled;
    if (googleMap != null) {
      updateMyLocationSettings();
    }
  }

  @Override
  public void setMyLocationButtonEnabled(boolean myLocationButtonEnabled) {
    if (this.myLocationButtonEnabled == myLocationButtonEnabled) {
      return;
    }
    this.myLocationButtonEnabled = myLocationButtonEnabled;
    if (googleMap != null) {
      updateMyLocationSettings();
    }
  }

  @Override
  public void setTrafficEnabled(boolean trafficEnabled) {
    if (this.trafficEnabled == trafficEnabled) {
      return;
    }
    this.trafficEnabled = trafficEnabled;
    if (googleMap != null) {
      googleMap.setTrafficEnabled(trafficEnabled);
    }
  }

  @Override
  public void setInitialMarkers(Object initialMarkers) {
    this.initialMarkers = (List<Object>) initialMarkers;
    if (googleMap != null) {
      updateInitialMarkers();
    }
  }

  private void updateInitialMarkers() {
    markersController.addMarkers(initialMarkers);
  }

  @Override
  public void setInitialPolygons(Object initialPolygons) {
    this.initialPolygons = (List<Object>) initialPolygons;
    if (googleMap != null) {
      updateInitialPolygons();
    }
  }

  private void updateInitialPolygons() {
    polygonsController.addPolygons(initialPolygons);
  }

  @Override
  public void setInitialPolylines(Object initialPolylines) {
    this.initialPolylines = (List<Object>) initialPolylines;
    if (googleMap != null) {
      updateInitialPolylines();
    }
  }

  private void updateInitialPolylines() {
    polylinesController.addPolylines(initialPolylines);
  }

  @Override
  public void setInitialCircles(Object initialCircles) {
    this.initialCircles = (List<Object>) initialCircles;
    if (googleMap != null) {
      updateInitialCircles();
    }
  }

  private void updateInitialCircles() {
    circlesController.addCircles(initialCircles);
  }

  @SuppressLint("MissingPermission")
  private void updateMyLocationSettings() {
    if(googleMap == null)
      return;

    if (hasLocationPermission()) {
      // The plugin doesn't add the location permission by default so that apps that don't need
      // the feature won't require the permission.
      // Gradle is doing a static check for missing permission and in some configurations will
      // fail the build if the permission is missing. The following disables the Gradle lint.
      //noinspection ResourceType
      googleMap.setMyLocationEnabled(myLocationEnabled);
      googleMap.getUiSettings().setMyLocationButtonEnabled(myLocationButtonEnabled);
    } else {
      retrySetPosition(10, 1);
      // TODO(amirh): Make the options update fail.
      // https://github.com/flutter/flutter/issues/24327
      Log.e(TAG, "Cannot enable MyLocation layer as location permissions are not granted");
    }
  }

  @SuppressLint("MissingPermission")
  private void retrySetPosition(final int limit, final int times) {
    if(times > limit)
      return;

    int secs = 1500;
    if(times == limit)
      secs = 15000;

    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {

        if (hasLocationPermission()) {
          googleMap.setMyLocationEnabled(false);
          googleMap.setMyLocationEnabled(myLocationEnabled);
          googleMap.getUiSettings().setMyLocationButtonEnabled(myLocationButtonEnabled);

        } else {
          retrySetPosition(limit, times + 1);
        }

      }
    }, secs);
  }

  private boolean hasLocationPermission() {
    return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
  }

  private int checkSelfPermission(String permission) {
    if (permission == null) {
      throw new IllegalArgumentException("permission is null");
    }
    return context.checkPermission(
        permission, android.os.Process.myPid(), android.os.Process.myUid());
  }
}
