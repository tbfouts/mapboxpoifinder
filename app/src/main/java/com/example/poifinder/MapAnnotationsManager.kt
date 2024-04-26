package com.example.poifinder

import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import com.example.poifinder.databinding.ViewAnnotationBinding
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.viewannotation.ViewAnnotationManager


/**
 * This class is used as a proxy to manage the creation and removal of map annotations on a given @MapView
 *
 * @param mapView the @MapView to create/remove annotations from
 */
class MapAnnotationsManager(mapView: MapView) {
    private val mMapView: MapView = mapView
    private val mPointAnnotationManager: PointAnnotationManager = mMapView?.annotations.createPointAnnotationManager()
    private val mViewAnnotationManager: ViewAnnotationManager = mMapView.viewAnnotationManager

    /**
     * This function adds a given @flickrImage to the map as a @PointAnnotation built with the flickrImage's bitmap.
     * When this point annotation is selected, a @ViewAnnotation is shown with the @flickrImage's title.
     *
     * @param flickrImage the image to create an annotation on the map for
     */
    fun addPointAnnotation(flickrImage: FlickrImage) {

        // pass the image title as data to the point annotation to be used later
        val data = JsonObject();
        data.add("title", JsonPrimitive(flickrImage.title));

        val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
            .withPoint(flickrImage.point)
            .withIconImage(flickrImage.bitmap)
            .withIconAnchor(IconAnchor.BOTTOM)
            .withData(data)
        val pointAnnotation = mPointAnnotationManager.create(pointAnnotationOptions)

        val viewAnnotation = mViewAnnotationManager.addViewAnnotation(
            resId = R.layout.view_annotation,
            options = viewAnnotationOptions {
                geometry(flickrImage.point)
                associatedFeatureId(pointAnnotation.featureIdentifier)
                anchor(ViewAnnotationAnchor.BOTTOM)
                offsetY((pointAnnotation.iconImageBitmap?.height!!).toInt())
            })

        // since visibility is bugged for ViewAnnotation, start with a blank text string
        ViewAnnotationBinding.bind(viewAnnotation).apply {
            textViewAnnotation.text  = ""
        }

        // when a point annotation is clicked, if the image title is not being shown, grab the given image title
        // from the clicked annotation and apply it to the ViewAnnotation
        mPointAnnotationManager.addClickListener { clickedAnnotation ->
            val view =
                mViewAnnotationManager.getViewAnnotationByFeatureId(clickedAnnotation.featureIdentifier)
            if (view != null) {
                ViewAnnotationBinding.bind(view).apply {
                    if (textViewAnnotation.text.isEmpty()) {
                        val caption =
                            clickedAnnotation.getData()?.asJsonObject?.get("title")?.asString;
                        textViewAnnotation.text = caption
                    } else {
                        textViewAnnotation.text = ""
                    }
                }
            }
            true
        }
    }
    /**
     * This function clears all annotations from the map.
     */
    fun clearAnnotations()
    {
        mPointAnnotationManager.deleteAll();
    }
}