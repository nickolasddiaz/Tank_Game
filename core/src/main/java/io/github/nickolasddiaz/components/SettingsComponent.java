package io.github.nickolasddiaz.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import static io.github.nickolasddiaz.utils.CollisionCategory.GAME_SETTINGS;

public class SettingsComponent implements Component {
    public float musicVolume;
    public float sfxVolume;
    public boolean IS_MOBILE;
    public boolean DEBUG;
    public boolean paused = false;
    public boolean AUTO_FIRE;
    public boolean is_Playing = false;

    public SettingsComponent(){
        Preferences pref = Gdx.app.getPreferences(GAME_SETTINGS);
        boolean mobile = Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS;
        musicVolume = pref.getFloat("musicVolume", .20f);
        sfxVolume = pref.getFloat("sfxVolume", .20f);
        IS_MOBILE = pref.getBoolean("IS_MOBILE", mobile);
        AUTO_FIRE = pref.getBoolean("AUTO_FIRE", mobile);
        DEBUG = pref.getBoolean("DEBUG", false);
    }
}
