package io.github.nickolasddiaz.gwt;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import io.github.nickolasddiaz.yourgame;

import static com.ibm.icu.impl.number.AffixPatternProvider.Flags.PADDING;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        // Resizable application, uses available space in browser with no padding:
        GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(true);
        cfg.padVertical = 0;
        cfg.padHorizontal = 0;
        return cfg;
        // If you want a fixed size application, comment out the above resizable section,
        // and uncomment below:
        // return new GwtApplicationConfiguration(640, 480);
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new yourgame();
    }

    class ResizeListener implements ResizeHandler {
        @Override
        public void onResize(ResizeEvent event) {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            } else {
                int width = event.getWidth() - PADDING;
                int height = event.getHeight() - PADDING;
                getRootPanel().setWidth("" + width + "px");
                getRootPanel().setHeight("" + height + "px");
                getApplicationListener().resize(width, height);
                Gdx.graphics.setWindowedMode(width, height);
            }
        }
    }


}
