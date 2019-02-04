package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import static com.fs.starfarer.api.GameState.COMBAT;

public class SRD_MusicBox extends BaseHullMod {

    private boolean MUSIC_PLAYING = false;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        CombatEngineAPI engine = Global.getCombatEngine();


        if (ship.getHullSpec().getHullId().contains("SRD_Metafalica")) {
            if (!MUSIC_PLAYING && ship.isAlive() && !engine.isSimulation()) {
                Global.getSoundPlayer().playCustomMusic(1, 1, "SRD_metafalica_theme", true);
                MUSIC_PLAYING = true;
            } else if (!ship.isAlive() || Global.getCurrentState() != COMBAT) {
                if (Global.getSoundPlayer().getCurrentMusicId().equals("SRD_metafalica_theme.ogg")) {
                    Global.getSoundPlayer().restartCurrentMusic();
                }
                MUSIC_PLAYING = false;
            }
        } else if (ship.getHullSpec().getHullId().contains("SRD_Celika_uv")) {
            if (!MUSIC_PLAYING && ship.isAlive() && Global.getCurrentState() == COMBAT) {
                Global.getSoundPlayer().playCustomMusic(1, 1, "SRD_takumi_theme", true);
                MUSIC_PLAYING = true;
            } else if (!ship.isAlive() || Global.getCurrentState() != COMBAT) {
                if (Global.getSoundPlayer().getCurrentMusicId().equals("SRD_takumi_theme.ogg")) {
                    Global.getSoundPlayer().restartCurrentMusic();
                }
                MUSIC_PLAYING = false;
            }
        }
    }

}
