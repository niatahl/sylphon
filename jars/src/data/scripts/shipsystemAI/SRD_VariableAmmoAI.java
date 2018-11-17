//Credit goes to Psiyon for his firecontrol AI script.
package data.scripts.shipsystemAI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.util.IntervalUtil;

public class SRD_VariableAmmoAI implements ShipSystemAIScript {

    private ShipSystemAPI system;
    private ShipAPI ship;

    //Sets an interval for once every 0.21-0.24 seconds. (meaning the code will only run once this interval has elapsed, not every frame)
    private final IntervalUtil tracker = new IntervalUtil(0.21f, 0.24f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);

        //Once the interval has elapsed...
        if (tracker.intervalElapsed()) {
            //No target: use shock cannons
            if (target == null) {
                setToAmmoType(1);
            }
            //Our target is small; use burst shells
            else if (target.getHullSize() == ShipAPI.HullSize.FIGHTER || target.getHullSize() == ShipAPI.HullSize.FRIGATE) {
                setToAmmoType(3);
            }
            //Big target with shielding: use our kine- IMPACT DRIVER
            else if (target.getShield() != null && target.getShield().isOn() && target.getShield().isWithinArc(ship.getLocation())) {
                setToAmmoType(2);
            }
            //Default: use shock cannons
            else {
                setToAmmoType(1);
            }
        }
    }

    private void activateSystem() {
        if (!system.isOn()) {
            ship.useSystem();
        }
    }

    private void setToAmmoType(int ammoNumber) {
        if (system.getAmmo() != ammoNumber && system.getCooldownRemaining() <= 0f) {
            activateSystem();
        }
    }
}
