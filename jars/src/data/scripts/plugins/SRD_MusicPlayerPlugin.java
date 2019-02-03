package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayList;
import java.util.List;

public class SRD_MusicPlayerPlugin extends BaseEveryFrameCombatPlugin {

    private static boolean MUSIC_PLAYING = false;
    private List<ShipAPI> trackedShips = new ArrayList<ShipAPI>();


    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        List<ShipAPI> untrack = new ArrayList<ShipAPI>();
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getHullSpec().getHullId().contains("SRD_Metafalica")
                    || ship.getHullSpec().getHullId().contains("SRD_Celika_uv")) {
                trackedShips.add(ship);
            }
        }

        for (ShipAPI ship : trackedShips) {
            if (ship.getHullSpec().getHullId().contains("SRD_Metafalica")) {
                if (!MUSIC_PLAYING && ship.isAlive()) {
                    Global.getSoundPlayer().playCustomMusic(1, 1, "SRD_metafalica_theme", true);
                    MUSIC_PLAYING = true;
                } else if (!ship.isAlive()) {
                    if (Global.getSoundPlayer().getCurrentMusicId().equals("SRD_metafalica_theme")) {
                        Global.getSoundPlayer().pauseCustomMusic();
                    }
                    MUSIC_PLAYING = false;
                    untrack.add(ship);
                }
            } else if (ship.getHullSpec().getHullId().contains("SRD_Celika_uv")) {
                if (!MUSIC_PLAYING && ship.isAlive()) {
                    Global.getSoundPlayer().playCustomMusic(1, 1, "SRD_takumi_theme", true);
                    MUSIC_PLAYING = true;
                } else if (!ship.isAlive()) {
                    if (Global.getSoundPlayer().getCurrentMusicId().equals("SRD_takumi_theme")) {
                        Global.getSoundPlayer().pauseCustomMusic();
                    }
                    MUSIC_PLAYING = false;
                    untrack.add(ship);
                }
            }
        }

        for (ShipAPI ship : untrack) {
            trackedShips.remove(ship);
        }

    }
}
