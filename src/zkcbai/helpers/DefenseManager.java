/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkcbai.helpers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.UnitDef;
import java.util.HashSet;
import java.util.Set;
import zkcbai.Command;
import zkcbai.EnemyDiscoveredListener;
import zkcbai.EnemyEnterLOSListener;
import zkcbai.UnitDestroyedListener;
import zkcbai.unitHandlers.units.AIUnit;
import zkcbai.unitHandlers.units.Enemy;

/**
 *
 * @author User
 */
public class DefenseManager extends Helper implements EnemyEnterLOSListener, UnitDestroyedListener, EnemyDiscoveredListener {

    public DefenseManager(Command cmd, OOAICallback clbk) {
        super(cmd, clbk);
        cmd.addEnemyEnterLOSListener(this);
        cmd.addUnitDestroyedListener(this);
        cmd.addEnemyDiscoveredListener(this);

        for (UnitDef ud : clbk.getUnitDefs()) {
            if (ud.getTooltip().toLowerCase().contains("riot") || ud.getTooltip().toLowerCase().contains("anti-swarm")) {
                riotDefs.add(ud);
            }
        }
    }

    private Set<Enemy> defenses = new HashSet();
    private Set<Enemy> riots = new HashSet();
    private Set<Enemy> fighters = new HashSet();

    private final Set<UnitDef> riotDefs = new HashSet();

    public float getDanger(AIFloat3 pos) {
        float result = 0;
        for (Enemy e : defenses) {
            result += (e.distanceTo(pos) <= e.getMaxRange() ? 1 : 0) * e.getDef().getCost(command.metal);
        }
        return result;
    }

    public float getImmediateDanger(AIFloat3 pos) {
        float result = 0;
        for (Enemy e : fighters) {
            /*if (e.getDef().getHumanName().contains("com")) {
                result += 200;
                continue;
            }*/
            result += (e.distanceTo(pos) <= e.getMaxRange() * 1.7 ? 1 : 0) * e.getDef().getCost(command.metal);
        }
        return result;
    }

    public float getGeneralDanger(AIFloat3 pos) {
        float result = 0;
        for (Enemy e : fighters) {
            result += (e.distanceTo(pos) <= e.getMaxRange() * 1.5 ? 0.5 : 0) * e.getDef().getCost(command.metal);
            result += (e.distanceTo(pos) <= e.getMaxRange() * 2.7 ? 0.5 : 0) * e.getDef().getCost(command.metal);
        }
        return result;
    }

    public float getDanger(AIFloat3 pos, float radius) {
        float result = 0;
        for (Enemy e : defenses) {
            result += (e.distanceTo(pos) <= (e.getMaxRange() + radius) ? 1 : 0) * e.getDef().getCost(command.metal);
        }
        return result;
    }

    public boolean isRaiderAccessible(AIFloat3 pos) {
        for (Enemy e : riots) {
            if (e.distanceTo(pos) < e.getMaxRange() * 1.8 && riotDefs.contains(e.getDef()) && !e.getUnit().isBeingBuilt()) {
                return false;
            }
        }
        return true;
    }

    public float getRaiderAccessibilityCost(AIFloat3 pos) {
        float res = 0;
        for (Enemy e : riots) {
            if (e.distanceTo(pos) < e.getMaxRange() * 1.8 && riotDefs.contains(e.getDef()) && !e.getUnit().isBeingBuilt()) {
                res += 1;
            } else if (e.distanceTo(pos) < e.getMaxRange() * 2.7 && riotDefs.contains(e.getDef()) && !e.getUnit().isBeingBuilt()) {
                res += 0.3;
            }
        }
        return res;
    }

    public Enemy getNearestRiot(AIFloat3 pos) {
        float minDist = Float.MAX_VALUE;
        Enemy best = null;
        for (Enemy e : riots) {
            if (e.distanceTo(pos) < minDist && riotDefs.contains(e.getDef()) && !e.getUnit().isBeingBuilt()) {
                minDist = e.distanceTo(pos);
                best = e;
            }
        }
        return best;
    }

    @Override
    public void unitFinished(AIUnit u) {
    }

    @Override
    public void update(int frame) {
    }

    @Override
    public void unitDestroyed(AIUnit u) {
    }

    @Override
    public void unitDestroyed(Enemy e) {
        defenses.remove(e);
        riots.remove(e);
        fighters.remove(e);
    }

    @Override
    public void enemyEnterLOS(Enemy e) {
        if (e.getDef().getSpeed() <= 0 && e.getDef().isAbleToAttack() && !defenses.contains(e)) {
            defenses.add(e);
        }
        if (e.getDef().isAbleToAttack()) {
            fighters.add(e);
        }
        if (riotDefs.contains(e.getDef())) {
            riots.add(e);
        } else if (riots.contains(e)) {
            riots.remove(e);//false positive
        }
    }

    @Override
    public void enemyDiscovered(Enemy e) {
        if (riotDefs.contains(e.getDef())) {
            riots.add(e);
        }
        if (e.getDef().isAbleToAttack() && e.isIdentifiedByRadar()) {
            fighters.add(e);
        }
    }

}
