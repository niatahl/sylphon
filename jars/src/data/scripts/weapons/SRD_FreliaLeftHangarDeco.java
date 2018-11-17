package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SRD_FreliaLeftHangarDeco implements EveryFrameWeaponEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }

        //Checks if our dedicated weapon slot is empty; if so, we switch to our "active" sprite. Otherwise, we switch to our "empty" sprite
        if (!ship.getVariant().getFittedWeaponSlots().contains("SLOT_SUPER_LEFT")) {
            weapon.getAnimation().setFrame(1);
        } else {
            weapon.getAnimation().setFrame(0);
        }
    }
}
