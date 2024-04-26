package com.example.poifinder;

import android.graphics.Bitmap;

import com.mapbox.geojson.Point;

public class FlickrImage {
    private String mTitle;
    private String mId;
    private String mServerId;
    private String mSecret;
    private Point mPoint;
    private Bitmap mBitmap;

    public FlickrImage(String title, String id, String serverId, String secret, Point point, Bitmap bitmap)
    {
        if(title.length() > 40)
        {
            this.mTitle = title.substring(0, 40);
        }
        else if(title.trim().length() == 0)
        {
            this.mTitle = id;
        }
        else
        {
            this.mTitle = title;
        }

        this.mId = id;
        this.mServerId = serverId;
        this.mSecret = secret;
        this.mPoint = point;
        this.mBitmap = bitmap;
    }

    public String getTitle() { return mTitle; }

    public String getId()
    {
        return mId;
    }

    public String getServerId()
    {
        return mServerId;
    }

    public String getSecret()
    {
        return mSecret;
    }

    public Point getPoint()
    {
        return mPoint;
    }

    public Bitmap getBitmap() { return mBitmap; }
}
