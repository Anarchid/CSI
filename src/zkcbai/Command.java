/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zkcbai;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import zkcbai.helpers.DefenseManager;
import zkcbai.helpers.ZoneManager;
import zkcbai.unitHandlers.CommanderHandler;
import zkcbai.unitHandlers.FactoryHandler;
import zkcbai.unitHandlers.units.AIUnit;
import zkcbai.helpers.LosManager;
import zkcbai.helpers.RadarManager;
import zkcbai.unitHandlers.units.Enemy;

/**
 *
 * @author User
 */
public class Command implements AI {

    private final OOAICallback clbk;
    private final int ownTeamId;
    public final Resource metal;
    public final Resource energy;

    public final LosManager losManager;
    public final RadarManager radarManager;
    public final ZoneManager areaManager;
    public final DefenseManager defenseManager;

    private final List<EnemyEnterRadarListener> enemyEnterRadarListeners = new ArrayList();
    private final List<EnemyEnterLOSListener> enemyEnterLOSListeners = new ArrayList();
    private final List<EnemyLeaveRadarListener> enemyLeaveRadarListeners = new ArrayList();
    private final List<EnemyLeaveLOSListener> enemyLeaveLOSListeners = new ArrayList();
    private final List<UnitFinishedListener> unitFinishedListeners = new ArrayList();
    private final List<UnitDestroyedListener> unitDestroyedListeners = new ArrayList();
    private final List<UpdateListener> updateListeners = new ArrayList();
    private final Map<Integer, List<UpdateListener>> singleUpdateListeners = new TreeMap();

    private final List<CommanderHandler> comHandlers = new ArrayList();
    private final FactoryHandler facHandler;

    private final Map<Integer, AIUnit> units = new TreeMap();
    private final Map<Integer, Enemy> enemies = new TreeMap();
    private final Set<UnitDef> enemyDefs = new HashSet();
    private final TreeMap<Float, UnitDef> defSpeedMap = new TreeMap();

    private int frame;

    public Command(int teamId, OOAICallback callback) {
        this.clbk = callback;
        ownTeamId = teamId;
        facHandler = new FactoryHandler(this, callback);
        metal = clbk.getResources().get(0);
        energy = clbk.getResources().get(1);
        losManager = new LosManager(this, clbk);
        areaManager = new ZoneManager(this, clbk);
        radarManager = new RadarManager(this, clbk);
        defenseManager = new DefenseManager(this, clbk);
    }

    public FactoryHandler getFactoryHandler() {
        return facHandler;
    }

    public TreeMap<Float, UnitDef> getEnemyUnitDefSpeedMap() {
        return defSpeedMap;
    }

    public Set<UnitDef> getEnemyUnitDefs() {
        return enemyDefs;
    }

    public int getCurrentFrame() {
        return frame;
    }

    public void addEnemyEnteredRadarListener(EnemyEnterRadarListener listener) {
        enemyEnterRadarListeners.add(listener);
    }

    public void addEnemyEnteredLOSListener(EnemyEnterLOSListener listener) {
        enemyEnterLOSListeners.add(listener);
    }

    public void addEnemyLeaveRadarListener(EnemyLeaveRadarListener listener) {
        enemyLeaveRadarListeners.add(listener);
    }

    public void addEnemyLeaveLOSListener(EnemyLeaveLOSListener listener) {
        enemyLeaveLOSListeners.add(listener);
    }

    public void addUnitFinishedListener(UnitFinishedListener listener) {
        unitFinishedListeners.add(listener);
    }

    public void addUnitDestroyedListener(UnitDestroyedListener listener) {
        unitDestroyedListeners.add(listener);
    }

    public void addSingleUpdateListener(UpdateListener listener, int frame) {
        List<UpdateListener> list = singleUpdateListeners.get(frame);
        if (list == null) {
            singleUpdateListeners.put(frame, new ArrayList());
        }
        singleUpdateListeners.get(frame).add(listener);
    }

    public void addUpdateListener(UpdateListener listener) {
        updateListeners.add(listener);
    }

    private void checkForMetal(String luamsg) {
        List<AIFloat3> availablemetalspots = new ArrayList();
        debug(luamsg.substring(12, luamsg.length()));
        for (String spotDesc : luamsg.substring(13, luamsg.length() - 2).split("},\\{")) {
            debug(spotDesc);
            float x, y, z;
            y = Float.parseFloat(spotDesc.split(",")[0].split(":")[1]);
            x = Float.parseFloat(spotDesc.split(",")[1].split(":")[1]);
            z = Float.parseFloat(spotDesc.split(",")[3].split(":")[1]);
            availablemetalspots.add(new AIFloat3(x, y, z));
        }

        areaManager.setMexSpots(availablemetalspots);
    }

    @Override
    public int luaMessage(String inData) {
        try {
            if (inData.length() > 11 && inData.substring(0, 11).equalsIgnoreCase("METAL_SPOTS")) {
                checkForMetal(inData);
            }
            return 0;
        } catch (Exception e) {
            debug("Exception in luaMessage: ", e);
        }
        return 0;
    }

    @Override
    public int unitGiven(Unit unit, int oldTeamId, int newTeamId) {
        try {
            unitFinished(unit);
        } catch (Exception e) {
            debug("Exception in unitGiven: ", e);
        }
        return 0;
    }

    @Override
    public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
        try {
        } catch (Exception e) {
            debug("Exception in unitDamaged: ", e);
        }
        return 0;
    }

    @Override
    public int enemyEnterLOS(Unit enemy) {
        try {
            if (!enemies.containsKey(enemy.getUnitId())) {
                enemies.put(enemy.getUnitId(), new Enemy(enemy, this, clbk));

            }
            if (!enemyDefs.contains(enemy.getDef())) {
                enemyDefs.add(enemy.getDef());
                debug("New enemy UnitDef: " + enemy.getDef().getHumanName());
                if (!defSpeedMap.containsKey(enemy.getMaxSpeed())) {
                    defSpeedMap.put(enemy.getMaxSpeed(), enemy.getDef());
                }
            }
            Enemy aiEnemy = enemies.get(enemy.getUnitId());
            aiEnemy.enterLOS();
            for (EnemyEnterLOSListener listener : enemyEnterLOSListeners) {
                listener.enemyEnterLOS(aiEnemy);
            }
//            debug("Health, Speed, Cost: ");
//            debug(enemy.getHealth() + "/" + enemy.getMaxHealth());
//            debug(enemy.getSpeed() + "/" + enemy.getMaxSpeed());
//            debug(enemy.getVel().length());
//            debug(enemy.getDef().getCost(metal));

        } catch (Exception e) {
            debug("Exception in enemyEnterLOS: ", e);
        }
        return 0;
    }

    @Override
    public int enemyLeaveLOS(Unit enemy) {
        try {
            Enemy aiEnemy = enemies.get(enemy.getUnitId());
            aiEnemy.leaveLOS();
            for (EnemyLeaveLOSListener listener : enemyLeaveLOSListeners) {
                listener.enemyLeaveLOS(aiEnemy);
            }
        } catch (Exception e) {
            debug("Exception in enemyLeaveLOS: ", e);
        }
        return 0;
    }

    @Override
    public int enemyEnterRadar(Unit enemy) {
        try {
            if (!enemies.containsKey(enemy.getUnitId())) {
                enemies.put(enemy.getUnitId(), new Enemy(enemy, this, clbk));
            }
            Enemy aiEnemy = enemies.get(enemy.getUnitId());
            aiEnemy.enterRadar();
            for (EnemyEnterRadarListener listener : enemyEnterRadarListeners) {
                listener.enemyEnterRadar(aiEnemy);
            }
//            debug("Health, Speed, Cost: ");
//            debug(enemy.getHealth() + "/" + enemy.getMaxHealth());
//            debug(enemy.getSpeed() + "/" + enemy.getMaxSpeed());
//            debug(enemy.getVel().length());
        } catch (Exception e) {
            debug("Exception in enemyEnterRadar: ", e);
        }
        return 0;
    }

    @Override
    public int enemyLeaveRadar(Unit enemy) {
        try {
            Enemy aiEnemy = enemies.get(enemy.getUnitId());
            aiEnemy.leaveRadar();
            for (EnemyLeaveRadarListener listener : enemyLeaveRadarListeners) {
                listener.enemyLeaveRadar(aiEnemy);
            }
        } catch (Exception e) {
            debug("Exception in enemyLeaveRadar: ", e);
        }
        return 0;
    }

    @Override
    public int enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
        try {
        } catch (Exception e) {
            debug("Exception in enemyDamaged: ", e);
        }
        return 0;
    }

    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
        try {
            AIUnit aiunit = units.get(unit.getUnitId());
            if (aiunit != null) {
                aiunit.destroyed();
                
                for (UnitDestroyedListener listener : unitDestroyedListeners) {
                    listener.unitDestroyed(aiunit);
                }
                
            }
            Enemy e = enemies.get(unit.getUnitId());
            if (e != null) {
                e.destroyed();
                
                for (UnitDestroyedListener listener : unitDestroyedListeners) {
                    listener.unitDestroyed(e);
                }
            }
        } catch (Exception e) {
            debug("Exception in unitDestroyed: ", e);
        }
        return 0;
    }

    @Override
    public int init(int teamId, OOAICallback callback) {
        return 0;
    }

    @Override
    public int unitMoveFailed(Unit unit) {
        try {
            units.get(unit.getUnitId()).pathFindingError();
        } catch (Exception e) {
            debug("Exception in unitMoveFailed: ", e);
        }
        return 0;
    }

    @Override
    public int unitIdle(Unit unit) {
        try {
            units.get(unit.getUnitId()).idle();
        } catch (Exception e) {
            debug("Exception in unitIdle: ", e);
        }
        return 0;
    }

    @Override
    public int update(int frame) {
        try {
            this.frame = frame;
            for (UpdateListener listener : updateListeners) {
                listener.update(frame);
            }
            if (singleUpdateListeners.containsKey(frame)) {
                for (UpdateListener listener : singleUpdateListeners.get(frame)) {
                    listener.update(frame);
                }
            }
            singleUpdateListeners.remove(frame);
        } catch (Exception e) {
            debug("Exception in update: ", e);
        }
        return 0;
    }

    @Override
    public int unitCreated(Unit unit, Unit builder) {
        try {
        } catch (Exception e) {
            debug("Exception in unitCreated: ", e);
        }
        return 0;
    }

    @Override
    public int unitFinished(Unit unit) {
        try {
            AIUnit aiunit;
            switch (unit.getDef().getName()) {
                case "armcom1":
                    CommanderHandler comHandler = new CommanderHandler(this, clbk);
                    comHandlers.add(comHandler);
                    aiunit = comHandler.addUnit(unit);
                    break;
                default:
                    debug("Unused UnitDef " + unit.getDef().getName() + " in UnitFinished");
                    aiunit = new AIUnit(unit, null);
            }
            units.put(unit.getUnitId(), aiunit);
            for (UnitFinishedListener listener : unitFinishedListeners) {
                listener.unitFinished(aiunit);
            }
        } catch (Exception e) {
            debug("Exception in unitFinished: ", e);
        }
        return 0;

    }

    public void debug(String s, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        debug(s + sw.toString());
    }

    public void debug(Integer s) {
        debug(s.toString());
    }

    public void debug(Float s) {
        debug(s.toString());
    }

    public void debug(String s) {
        clbk.getGame().sendTextMessage(s, clbk.getGame().getMyTeam());
    }

    public void mark(AIFloat3 pos, String s) {
        clbk.getMap().getDrawer().addPoint(pos, s);
    }
}
