package com.example.poifinder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbox.geojson.Point;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * A @FlickrTask is used to find search results from the Flickr API based on a given search query.  Based on the results of
 * the initial search, this task then performs additional requests to the Flickr API to obtain location information as well as a bitmap
 * representation of the image found.  Search results are published back via a @FlickrTaskCallback.
 */
public class FlickrTask implements Runnable {

    // the initial search url
    private String mSearchUrl;

    // the callback for search results
    private FlickrTaskCallback mCallback;

    /**
     * The FlickrTaskCallback is used to communicate results from a FlickrTask back to it's owner.
     * Results are published on the thread this FlickrTask is executed from.
     */
    public interface FlickrTaskCallback
    {
        /**
         * Called when the search task is completed and all images and their data has been found
         *
         * @param searchResults a comprehensive list of all search results
         */
        void onComplete(ArrayList<FlickrImage> searchResults);

        /**
         * Called each time a new image is found
         *
         * @param newImage the newly found flickr image
         */
        void onUpdate(FlickrImage newImage);

        /**
         * Called when the search task fails for any reason
         *
         * @param reason the reason the search failed
         */
        void onFail(String reason);
    }

    /**
     * Constructs a FlickrTask object for the given query/center point and registers a callback.
     *
     * @param searchQuery the text to search for
     * @param centerPoint the center point to search around
     * @param callback the @FlickrTaskCallback to register with this task
     */
    public FlickrTask(String searchQuery, Point centerPoint, FlickrTaskCallback callback)
    {
        this.mCallback = callback;

        // quick and dirty way to build the url - would create a URL builder class with more time
        mSearchUrl = "https://www.flickr.com/services/rest/?method=flickr.photos.search&api_key=3e7cc266ae2b0e0d78e279ce8e361736&text="
                + searchQuery + "&lat=" + centerPoint.latitude() + "&lon=" + centerPoint.longitude() + "&radius=20&radius_units=mi&per_page=25&format=json&nojsoncallback=1";
    }

    @Override
    public void run() {
        ArrayList<FlickrImage> flickrImages = new ArrayList<>();
        try {
            URL url = new URL(mSearchUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            JsonObject jsonObject = (JsonObject)JsonParser.parseReader(new InputStreamReader(in, "UTF-8"));
            JsonObject photosJson = jsonObject.getAsJsonObject("photos");
            JsonArray photoJson = photosJson.getAsJsonArray("photo");

            // for each photo, parse the json data and then make follow up requests for image location and image bitmap
            for(int x = 0; x < photoJson.size(); x++)
            {
                JsonObject obj = photoJson.get(x).getAsJsonObject();
                if(null != obj) {
                    String title = obj.get("title").getAsString();
                    String id = obj.get("id").getAsString();
                    String server = obj.get("server").getAsString();
                    String secret = obj.get("secret").getAsString();
                    FlickrImage flickrImage = new FlickrImage(title, id, server, secret, getImageLocation(obj.get("id").getAsString()), getImage(server, id, secret));
                    flickrImages.add(flickrImage);
                    mCallback.onUpdate(flickrImage);
                }
            }

            urlConnection.disconnect();
            mCallback.onComplete(flickrImages);
        }
        catch (Exception e)
        {
            mCallback.onFail(e.toString());
        }
    }

    /**
     * This funtion returns the bitmap of a flicker image
     *
     * @param serverId the server id of the flickr image
     * @param id the id of the flickr image
     * @param secret the secret of the flickr image
     * @return the bitmap of the flicker image (if an exception occurs a generic empty bitmap is returned)
     */
    private Bitmap getImage(String serverId, String id, String secret)
    {
        Bitmap bitmap = null;

        try {
            // quick and dirty url, would create a builder with more time
            URL url = new URL("https://live.staticflickr.com/" + serverId + "/" + id + "_" + secret + "_q.jpg");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            bitmap = BitmapFactory.decodeStream(in);
            urlConnection.disconnect();
        }
        catch (Exception e)
        {
            Log.e("Caught exception", e.toString());
            bitmap = Bitmap.createBitmap(75, 75, Bitmap.Config.ARGB_8888);
        }

        return bitmap;
    }

    /**
     * This funtion returns the location @Point of a flicker image
     *
     * @param id the ID of the image to search for location
     * @return the point where the image is located
     * @throws IOException if failure occurs
     */
    private Point getImageLocation(String id) throws IOException {
        Point point = null;
        try {
            // quick and dirty url, would create a builder with more time
            URL url = new URL("https://api.flickr.com/services/rest/?method=flickr.photos.geo.getLocation&api_key=3e7cc266ae2b0e0d78e279ce8e361736&format=json&nojsoncallback=1&photo_id=" + id);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            JsonObject jsonObject = (JsonObject) JsonParser.parseReader(new InputStreamReader(in,"UTF-8"));

            JsonObject photo = jsonObject.getAsJsonObject("photo");
            JsonObject location = photo.getAsJsonObject("location");
            point = Point.fromLngLat(location.get("longitude").getAsDouble(), location.get("latitude").getAsDouble());
            Log.e("location", location.toString());
            urlConnection.disconnect();
        }
        catch (IOException e)
        {
            throw e;
        }

        return point;
    }
}
