//By Nicke535, extremely rudimentary missile AI for Exalt-class missiles.
//So rudimentary, in fact, that it isn't used!
package data.scripts.missileAI;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;

public class SRD_ExaltMissileAI implements GuidedMissileAI, MissileAIPlugin {

    private CombatEntityAPI target;

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void advance(float amount) {

    }
}
