package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public class SettingsComponent implements Component {
    public int musicVolume = 50;
    public int sfxVolume = 50;
    public boolean IS_MOBILE = Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS;;
    public boolean DEBUG = false;
    public boolean paused = false;
}
