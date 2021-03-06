package fast.rocket.cache;

import com.android.rocket.R;

import fast.rocket.cache.ImageLoader.ImageCallback;
import fast.rocket.cache.ImageLoader.ImageContainer;
import fast.rocket.cache.ImageLoader.ImageListener;
import fast.rocket.config.CacheViewConfig;
import fast.rocket.error.RocketError;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

/**
 * The Class CircularCacheView.
 */
public class CircularCacheView extends ImageView {

/** The skip disk cache. */
private boolean skipDiskCache;
	
    /** The URL of the network image to load. */
    private String mUrl;

	/** The max width. */
	private int maxWidth;
	
	/** The max height. */
	private int maxHeight;

    /** Local copy of the ImageLoader. */
    private ImageLoader mImageLoader;

    /** Current ImageContainer. (either in-flight or finished) */
    private ImageContainer mImageContainer;
    
    /** The callback. */
    private ImageCallback callback;
    
    /** The config. */
    private CacheViewConfig config;
    
    
	/** The bitmap. */
	private Bitmap bitmap;
	
	/** The border width. */
	private float borderWidth = 2.0F;
	
	/** The center. */
	private float center;
	
	/** The draw border. */
	private boolean drawBorder = true;
	
	/** The height. */
	private int height;
	
	/** The paint. */
	private Paint paint;
	
	/** The paint border. */
	private Paint paintBorder;
	
	/** The shader. */
	private BitmapShader shader;
	
	/** The width. */
	private int width;

	/**
	 * Instantiates a new circular cache view.
	 *
	 * @param paramContext the param context
	 */
	public CircularCacheView(Context paramContext) {
		super(paramContext);
		setup();
	}

	/**
	 * Instantiates a new circular cache view.
	 *
	 * @param paramContext the param context
	 * @param paramAttributeSet the param attribute set
	 */
	public CircularCacheView(Context paramContext,
			AttributeSet paramAttributeSet) {
		super(paramContext, paramAttributeSet);
		setup();
	}

	/**
	 * Instantiates a new circular cache view.
	 *
	 * @param paramContext the param context
	 * @param paramAttributeSet the param attribute set
	 * @param paramInt the param int
	 */
	public CircularCacheView(Context paramContext,
			AttributeSet paramAttributeSet, int paramInt) {
		super(paramContext, paramAttributeSet, paramInt);
		setup();
	}

	/**
	 * Sets the shader.
	 */
	private void setShader() {
		BitmapDrawable drawable = (BitmapDrawable) getDrawable();
		if(drawable != null) {
			this.bitmap = drawable.getBitmap();
		}
		
		if ((this.bitmap != null) && (this.width > 0) && (this.height > 0)) {
			this.shader = new BitmapShader(Bitmap.createScaledBitmap(
					this.bitmap, this.width, this.height, false),
					BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
			this.paint.setShader(this.shader);
		}
	}

	/**
	 * Setup.
	 */
	private void setup() {
		Resources localResources = getResources();
		this.borderWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				this.borderWidth, localResources.getDisplayMetrics());
		this.paint = new Paint();
		this.paint.setAntiAlias(true);
		this.paintBorder = new Paint();
		this.paintBorder.setColor(localResources.getColor(R.color.paint_border));
		this.paintBorder.setStyle(Paint.Style.STROKE);
		this.paintBorder.setStrokeWidth(this.borderWidth);
		this.paintBorder.setAntiAlias(true);
	}

	/* (non-Javadoc)
	 * @see android.widget.ImageView#onDraw(android.graphics.Canvas)
	 */
	public void onDraw(Canvas paramCanvas) {
		BitmapDrawable drawable = (BitmapDrawable) getDrawable();
		if (drawable == null) {
			config.placeholder();
		} else if ((drawable.getBitmap() == null)
				|| (drawable.getBitmap().isRecycled())) {
			config.placeholder();
		}
		try {
			drawCircle(paramCanvas);
		} catch (RuntimeException localRuntimeException) {
		}
	}
	
	/**
	 * Sets URL of the image that should be loaded into this view. Note that calling this will
	 * immediately either set the cached image (if available) or the default image specified by
	 *
	 * @param url The URL that should be loaded into this ImageView.
	 * @param imageLoader ImageLoader that will be used to make the request.
	 * @param maxWidth the max width
	 * @param maxHeight the max height
	 * @param skipDiskCache the skip disk cache
	 * @param callback the callback
	 * @param config the config
	 * {@link NetworkCacheView#setDefaultImageResId(int)} on the view.
	 * 
	 * NOTE: If applicable, {@link NetworkCacheView#setDefaultImageResId(int)} and
	 * {@link NetworkCacheView#setErrorImageResId(int)} should be called prior to calling
	 * this function.
	 */
	public void setImageUrl(String url, ImageLoader imageLoader, int maxWidth, int maxHeight, 
			boolean skipDiskCache, final ImageCallback callback, CacheViewConfig config) {
        this.mUrl = url;
        this.mImageLoader = imageLoader;
        this.skipDiskCache = skipDiskCache;
        this.callback = callback;
        this.config = config;
        // The URL has potentially changed. See if we need to load it.
        loadImageIfNecessary(false);
    }
	
	/**
	 * Draw circle.
	 *
	 * @param paramCanvas the param canvas
	 */
	private void drawCircle(Canvas paramCanvas) {
		if ((this.bitmap != null) && (this.shader != null)) {
			float f1 = this.center - 2 * (int) this.borderWidth;
			float f2 = this.center - ((int) this.borderWidth >> 1);
			paramCanvas.drawCircle(this.center, this.center, f1, this.paint);
			if (this.drawBorder)
				paramCanvas.drawCircle(this.center, this.center, f2
						- this.borderWidth, this.paintBorder);
		}
	}

    /**
     * Loads the image for the view if it isn't already loaded.
     * @param isInLayoutPass True if this was invoked from a layout pass, false otherwise.
     */
    private void loadImageIfNecessary(final boolean isInLayoutPass) {
        int width = getWidth();
        int height = getHeight();

        boolean isFullyWrapContent = getLayoutParams() != null
                && getLayoutParams().height == LayoutParams.WRAP_CONTENT
                && getLayoutParams().width == LayoutParams.WRAP_CONTENT;
        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
        if (width == 0 && height == 0 && !isFullyWrapContent) {
            return;
        }

        // if the URL to be loaded in this view is empty, cancel any old requests and clear the
        // currently loaded image.
        if (TextUtils.isEmpty(mUrl)) {
            if (mImageContainer != null) {
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            setImageBitmap(null);
            return;
        }

        // if there was an old request in this view, check if it needs to be canceled.
        if (mImageContainer != null && mImageContainer.getRequestUrl() != null) {
            if (mImageContainer.getRequestUrl().equals(mUrl)) {
                // if the request is from the same URL, return.
                return;
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                mImageContainer.cancelRequest();
                setImageBitmap(null);
            }
        }

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.
        ImageContainer newContainer = mImageLoader.get(mUrl,
                new ImageListener() {
                    @Override
                    public void onErrorResponse(RocketError error) {
                        config.error();
                    	
                        if(callback != null) {
        					callback.onComplete(CircularCacheView.this, null, false);
        				}
                    }

                    @Override
                    public void onResponse(final ImageContainer response, boolean isImmediate) {
                        // If this was an immediate response that was delivered inside of a layout
                        // pass do not set the image immediately as it will trigger a requestLayout
                        // inside of a layout. Instead, defer setting the image by posting back to
                        // the main thread.
                        if (isImmediate && isInLayoutPass) {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    onResponse(response, false);
                                }
                            });
                            return;
                        }

                        if (response.getBitmap() != null) {
                            setImageBitmap(response.getBitmap());
                            if(callback != null) {
								callback.onComplete(CircularCacheView.this,
										response.getBitmap(), false);
            				}else {
            					config.animateLoad();
            				}
                        } else{
                        	config.placeholder();
                        }
                    }
                }, maxWidth, maxHeight, skipDiskCache, callback);

        // update the ImageContainer to be the new bitmap container.
        mImageContainer = newContainer;
    }

    /* (non-Javadoc)
     * @see android.view.View#onLayout(boolean, int, int, int, int)
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true);
    }

	/* (non-Javadoc)
	 * @see android.view.View#onSizeChanged(int, int, int, int)
	 */
	protected void onSizeChanged(int paramInt1, int paramInt2, int paramInt3,
			int paramInt4) {
		super.onSizeChanged(paramInt1, paramInt2, paramInt3, paramInt4);
		this.width = paramInt1;
		this.height = paramInt2;
		this.center = (this.width >> 1);
		setShader();
	}

	/**
	 * Sets the draw border.
	 *
	 * @param paramBoolean the new draw border
	 */
	public void setDrawBorder(boolean paramBoolean) {
		this.drawBorder = paramBoolean;
		invalidate();
	}

	/* (non-Javadoc)
	 * @see android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)
	 */
	public void setImageDrawable(Drawable paramDrawable) {
		super.setImageDrawable(paramDrawable);
		if ((paramDrawable instanceof BitmapDrawable)) {
			this.bitmap = ((BitmapDrawable) paramDrawable).getBitmap();
			setShader();
			return;
		}
		this.shader = null;
		invalidate();
	}
}