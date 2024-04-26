package com.example.poifinder;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    // Reference to the Mapbox MapView
    private MapView mapView = null;

    // Reference to the most recently returned list of images from a Flicker search
    private ArrayList<FlickrImage> flickrImages = new ArrayList<>();

    // Reference to the most recent FlickrSearch task (used to determine if there is an ongoing search)
    private FutureTask<Void> flickrSearchFutureTask;

    // Reference to the MapAnnotationsManager used to add/remove annotations to the @mapView based on search results
    private MapAnnotationsManager mapAnnotationsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        SearchView searchView = findViewById(R.id.menu_search);
        ListView listView = findViewById(R.id.listViewResults);
        FloatingActionButton fabShowList = findViewById(R.id.fab_toggle_list);
        FloatingActionButton fabShowAnnotations = findViewById(R.id.fab_show_annotations);
        mapAnnotationsManager = new MapAnnotationsManager(mapView);

        mapView.getMapboxMap().loadStyleUri(Style.DARK);

        ArrayList<String> arrayList = new ArrayList<String>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, arrayList);
        listView.setAdapter(adapter);

        // Per requirements, when a list item is selected, close the list view and center the camera on that list item
        listView.setOnItemClickListener((AdapterView<?> adapterView, View view, int i, long l)->{
                setCameraOnPoint(flickrImages.get(i).getPoint(), 15.0);
                listView.setVisibility(View.INVISIBLE);
        });

        // Per the requirements, when this button is pressed, center the camera such that all annotations are in view
        fabShowAnnotations.setOnClickListener((View view)->{
            setCameraOnSearchResults();
        });

        // Per the requirements, show a list of search results to the user on button press
        fabShowList.setOnClickListener((View view)->{
            if(View.VISIBLE == listView.getVisibility())
            {
                listView.setVisibility(View.INVISIBLE);
            }
            else
            {
                listView.setVisibility(View.VISIBLE);
            }
        });

        // The @FlickrTaskCallback that defines what to do when we get search results back from the @FlickrTask
        FlickrTask.FlickrTaskCallback flickrTaskCallback = new FlickrTask.FlickrTaskCallback() {
            @Override
            public void onComplete(ArrayList<FlickrImage> searchResults) {
                // now that the search is complete, inform the user and center the map on the annotations that were created from the search
                mapView.post(() ->{
                    Toast.makeText(getApplicationContext(), "Search complete", Toast.LENGTH_LONG).show();
                    setCameraOnSearchResults();
                });
            }

            @Override
            public void onUpdate(FlickrImage newImage) {
                // as we get images, add them to our internal list and post the new result to the annotationManager and the listView
                flickrImages.add(newImage);

                mapView.post(() -> {
                    mapAnnotationsManager.addPointAnnotation(newImage);
                });

                listView.post(() -> {
                    arrayList.add(newImage.getTitle());
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onFail(String reason) {
                Log.e("FlickrTaskFailed", reason);
            }
        };

        // This callback takes a user's search query and uses it to initialize a flickr search task
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                // if there is an ongoing search, inform the user and return gracefully
                // (functionality could be added later to cancel the ongoing search and start a new one)
                if(null != flickrSearchFutureTask && !flickrSearchFutureTask.isDone())
                {
                    Toast.makeText(getApplicationContext(), "Please wait for current search to complete", Toast.LENGTH_LONG).show();
                    return false;
                }

                // before starting a new search, reset existing objects and views
                flickrImages.clear();
                arrayList.clear();
                mapAnnotationsManager.clearAnnotations();

                Toast.makeText(getApplicationContext(), "Searching for: \"" + query + "\" images nearby...", Toast.LENGTH_LONG).show();

                // create a new FlickrTask from the query, using the callback defined above and  then submit it to a worker thread (to avoid blocking the main UI thread during search
                FlickrTask flickrTask = new FlickrTask(query, mapView.getMapboxMap().getCameraState().getCenter(), flickrTaskCallback);
                flickrSearchFutureTask = (FutureTask<Void>) Executors.newSingleThreadExecutor().submit(flickrTask);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    /**
     * Sets the @mapView camera on a specific point at a specific zoom level.
     *
     * @param point the point to center the camera on
     * @param zoomLevel the zoom level to set the camera to
     */
    private void setCameraOnPoint(Point point, double zoomLevel)
    {
        mapView.getMapboxMap().setCamera(new CameraOptions.Builder().center(point).zoom(zoomLevel).build());
    }

    /**
     * Sets the camera to properly fit all @flickrImages is frame.
     */
    private void setCameraOnSearchResults()
    {
        List<Point> pointList = flickrImages.stream().map(FlickrImage::getPoint).collect(Collectors.toList());

        Polygon polygon = Polygon.fromLngLats(new ArrayList<>(Collections.singleton(pointList)));
        CameraOptions cameraOptions = mapView.getMapboxMap().cameraForGeometry(polygon, new EdgeInsets(200.0, 200.0, 200.0, 200.0), null, null);
        mapView.getMapboxMap().setCamera(cameraOptions);
    }
}