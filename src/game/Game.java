/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import dat.EfsIni;
import dat.Damage;
import dat.StrBuild;
import dat.Target;
import dat.TerrCost;
import dat.UnitType;
import dat.ResType;
import galaxyreader.Galaxy;
import galaxyreader.JumpGate;
import galaxyreader.Planet;
import galaxyreader.Structure;
import galaxyreader.Unit;
import gui.Resource;
import java.awt.Point;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import util.C;
import util.StackIterator;
import util.Util;
import util.Util.HexIter;

/**
 *
 * @author joulupunikki
 */
public class Game implements Serializable {

    //coordinates of currently selected hex/square/stack  
    private Point selected_point;
    //faction of selected space stack
    private int selected_faction;
    // the upper left corner of the planet map
    private Point planet_map_origin;    // Not used? RSW
    private Point space_map_origin;    // Not used? RSW
    // the travel path of the selected stack
    private LinkedList<Hex> path;
    // the travel path of the selected space stack
    private JumpGate jump_path;
//    private PlanetGrid[] planet_grids;
    private int current_planet;
    //faction whose turn it is
//    private int current_faction;    // Not used? RSW
    //game year
    private int year;
    //turn ie the current faction id
    private int turn;
    private boolean[] human_ctrl;
    private Galaxy galaxy;
    private GalaxyGrid galaxy_grid;
    private UnitType[][] unit_types;
    private ResType[] res_types;
    private StrBuild[] str_build;
    private double[][][] terr_cost;
//    private int max_spot_range;
    private GameResources game_resources;

//    private List<Unit> current_stack;
    private List<Planet> planets;
    private List<JumpGate> jump_gates;
    private List<Unit> units;
    private List<Structure> structures;

    private Faction[] factions = new Faction[C.NR_FACTIONS];    // RSW
    private List<Unit> unmoved_units;
    private List<Unit> cargo_pods;
    private List<Structure> faction_cities;

//    private List<Unit> combat_stack_a;
//    private List<Unit> combat_stack_b;
//    private String combat_type;
//    private int attacked_faction;
//    
    private int[][] damage;
    private int[][] target;

    private Random random;
    private EfsIni efs_ini;
    private Resources resources;
    private Economy economy;
    private Battle battle;
    private HexProc hex_proc;

    public Game(String galaxy_file, int current_planet) {

        random = new Random(1234567890L);

        // Read fixed data files
        unit_types = UnitType.readUnitDat();
        res_types = ResType.readResDat();
        str_build = StrBuild.readStrBuildDat();
        terr_cost = TerrCost.readTerrCost();
        damage = Damage.readDamageDat();
        target = Target.readTargetDat();

        galaxy = Galaxy.loadGalaxy(galaxy_file);
        planet_map_origin = new Point(0, 0);
        space_map_origin = new Point(0, 0);

        planets = galaxy.getPlanets();
        jump_gates = galaxy.getJumpGates();
        units = galaxy.getUnits();
        structures = galaxy.getStructures();
        galaxy_grid = new GalaxyGrid(galaxy);
        human_ctrl = new boolean[14];
        human_ctrl[0] = true;
        unmoved_units = new LinkedList<>();
        faction_cities = new LinkedList<>();
        this.current_planet = current_planet;
        year = C.STARTING_YEAR;
        turn = -1;
        hex_proc = new HexProc(this);

        game_resources = new GameResources();
//        Structure.setCanBuild(unit_types);
//        factions = Faction.createFactions();

        placeUnits();
        placeStructures();
        resetMovePoints();
        resetUnmovedUnits();

        setMoveType();
        setUnitTypeData();
        setJumpRoutes();
        initVisibility();
        setMaxSpotRange();

        battle = new Battle();

        for (int i = 0; i < str_build.length; i++) {
            System.out.println("str_build = " + str_build[i].name);
        }

        Damage.printDamage(damage);

        Target.printTarget(target);

        printMoveCost();

//        endTurn();
//        setMoveCosts();
    }

    public void init(Resource gui_resource) {

        battle.battleInit(random, damage, target, terr_cost, this, planets);

        efs_ini = EfsIni.readEfsIni(gui_resource.getEFSIni(), gui_resource.getPhoenixIni());
        resources = new Resources(this);
        economy = new Economy(this, resources);

        for (int i = 0; i < C.NR_FACTIONS; i++) {
            factions[i] = new Faction(this);
        }
    }

    public void initVisibility() {
        for (Planet planet : planets) {
            Hex[][] planet_grid = planet.planet_grid.getMapArray();
            for (int i = 0; i < planet_grid.length; i++) {
                for (int j = 0; j < planet_grid[i].length; j++) {
                    planet_grid[i][j].initVisibility();

                }

            }

        }

//        for (Structure structure : structures) {
//            Hex hex = getPlanetGrid(structure.p_idx).getHex(structure.x, structure.y);
//            hex_proc.hexProc(hex, 5, structure.owner, C.INIT_SPOT);
//        }
        for (Planet planet : planets) {
            Hex[][] planet_grid = planet.planet_grid.getMapArray();
            for (int i = 0; i < planet_grid.length; i++) {
                for (int j = 0; j < planet_grid[i].length; j++) {
                    Hex hex = planet_grid[i][j];
                    hex_proc.initSpotProc(hex, planet);

                }

            }
            List<Unit>[] stacks = planet.space_stacks;
            for (int i = 0; i < stacks.length; i++) {
                if (!stacks[i].isEmpty()) {
                    planet.spotted[i] = true;
                    for (int j = 0; j < stacks.length; j++) {
                        List<Unit> stack = stacks[j];
                        for (Unit unit : stack) {
                            unit.spotted[i] = true;
                        }

                    }
                }
            }
        }

    }

//    public void setMoveCosts() {
//
//
//        for (Planet e : planets) {
//
//            PathFind.setMoveCosts(e.planet_grid, terr_cost, e.tile_set_type);
//        }
//    }
    public List<JumpGate> getJumpGates() {
        return jump_gates;
    }

    public Resources getResources() {
        return resources;
    }

    public Economy getEconomy() {
        return economy;
    }

    public void setResearch(int tech) {
        factions[turn].getResearch().setResearch(tech);
    }

    /**
     * Called at the beginning of a turn only. Initializes research points and
     * researches selected tech.
     */
    public void doResearch() {
        factions[turn].getResearch().initResearchPts();
        factions[turn].getResearch().doResearch();
    }

    public void deleteUnit2(Unit u) {
        units.remove(u);
        unmoved_units.remove(u);
    }

    public List<Unit> getCombatStack(String stack) {
        return battle.getCombatStack(stack);
//        List<Unit> rv = null;
//        switch (stack) {
//            case "a":
//                rv = combat_stack_a;
//                break;
//            case "b":
//                rv = combat_stack_b;
//                break;
//            default:
//                throw new AssertionError();
//        }
//        return rv;
    }

    public Battle getBattle() {
        return battle;
    }

//    public void chooseOffence(List<Unit> attacker, List<Unit> attacker_offence, int i) {
//        for (Unit unit : attacker) {
//            switch (i) {
//                case C.WATER:
//                    if (unit.type_data.water_str > 0 && unit.health > 0 && !unit.routed) {
//                        attacker_offence.add(unit);
//                    }
//                    break;
//                case C.INDIRECT:
//                    if (unit.type_data.indirect_str > 0 && unit.health > 0 && !unit.routed) {
//                        attacker_offence.add(unit);
//                    }
//                    break;
//                case C.AIR:
//                    if (unit.type_data.air_str > 0 && unit.health > 0 && !unit.routed) {
//                        attacker_offence.add(unit);
//                    }
//                    break;
//
//                case C.DIRECT:
//                    if (unit.type_data.direct_str > 0 && unit.health > 0 && !unit.routed) {
//                        attacker_offence.add(unit);
//                    }
//                    break;
//                case C.CLOSE:
//                    if (unit.type_data.close_str > 0 && unit.health > 0 && !unit.routed) {
//                        attacker_offence.add(unit);
//                    }
//                    break;
//                case C.PSYCH:
//                    if (unit.type_data.psy_str > 0 && unit.health > 0 && !unit.routed) {
//                        attacker_offence.add(unit);
//                    }
//                    break;
//                case C.RANGED_SPACE:
//                    if (unit.type_data.ranged_sp_str > 0 && unit.health > 0 && !unit.routed) {
//                        attacker_offence.add(unit);
//                    }
//                    break;
//
//                case C.DIRECT_SPACE:
//                    if (unit.type_data.direct_sp_str > 0 && unit.health > 0 && !unit.routed) {
//                        attacker_offence.add(unit);
//                    }
//                    break;
//                case C.CLOSE_SPACE:
//                    if (unit.type_data.close_sp_str > 0 && unit.health > 0 && !unit.routed) {
//                        attacker_offence.add(unit);
//                    }
//                    break;
//                default:
//                    throw new AssertionError();
//            }
//        }
//
//    }
//
//    public void chooseDefence(List<Unit> defender, List<Unit> defender_defence, int i) {
//        for (Unit unit : defender) {
//            if (target[unit.move_type.ordinal()][i] == 1 && unit.health > 0 && !unit.routed) {
//                // what does this do ?
////                if (unit.type_data.move_type == C.MoveType.SPACE
////                        || unit.type_data.move_type == C.MoveType.JUMP
////                        || unit.type_data.move_type == C.MoveType.LANDER
////                        || i != C.RANGED_SPACE) {
//                defender_defence.add(unit);
////                }
//            }
//        }
//    }
//
//    public int getAtkAcc(int atk_type, Unit atk) {
//        int to_hit = 0;
//        System.out.println("attack_type " + atk_type);
//        switch (atk_type) {
//            case C.WATER:
//                to_hit += atk.type_data.water_acc;
//                break;
//            case C.INDIRECT:
//                to_hit += atk.type_data.indirect_acc;
//                break;
//            case C.AIR:
//                to_hit += atk.type_data.air_acc;
//                break;
//            case C.DIRECT:
//                to_hit += atk.type_data.direct_acc;
//                break;
//            case C.CLOSE:
//                to_hit += atk.type_data.close_acc;
//                break;
//            case C.PSYCH:
//                to_hit += atk.type_data.psy_acc;
//                break;
//            case C.RANGED_SPACE:
//                to_hit += atk.type_data.ranged_sp_acc;
//                break;
//            case C.DIRECT_SPACE:
//                to_hit += atk.type_data.direct_sp_acc;
//                break;
//            case C.CLOSE_SPACE:
//                to_hit += atk.type_data.close_sp_acc;
//                break;
//            default:
//                throw new AssertionError();
//        }
//        return to_hit;
//    }
//
//    public int getDam(Unit atk, Unit def, int atk_type) {
//        int dmg = 0;
//        double ratio = 1.0 / def.type_data.armor;
//        switch (atk_type) {
//            case C.WATER:
//                ratio *= atk.type_data.water_str;
//                break;
//            case C.INDIRECT:
//                ratio *= atk.type_data.indirect_str;
//                break;
//            case C.AIR:
//                ratio *= atk.type_data.air_str;
//                break;
//            case C.DIRECT:
//                ratio *= atk.type_data.direct_str;
//                break;
//            case C.CLOSE:
//                ratio *= atk.type_data.close_str;
//                break;
//            case C.PSYCH:
//                ratio = atk.type_data.psy_str * 1.0 / def.type_data.psy_def;
//                break;
//            case C.RANGED_SPACE:
//                ratio *= atk.type_data.ranged_sp_str;
//                break;
//            case C.DIRECT_SPACE:
//                ratio *= atk.type_data.direct_sp_str;
//                break;
//            case C.CLOSE_SPACE:
//                ratio *= atk.type_data.close_sp_str;
//                break;
//            default:
//                throw new AssertionError();
//        }
//
//        int dmg_index = -1;
//
//        if (ratio < 3) {
//            if (ratio < .25) {
//                dmg_index = 0;
//            } else if (ratio < .33) {
//                dmg_index = 1;
//            } else if (ratio < .5) {
//                dmg_index = 2;
//            } else if (ratio < 1) {
//                dmg_index = 3;
//            } else if (ratio < 2) {
//                dmg_index = 4;
//            } else {
//                dmg_index = 5;
//            }
//        } else {
//            if (ratio < 4) {
//                dmg_index = 6;
//            } else if (ratio < 5) {
//                dmg_index = 7;
//            } else if (ratio < 6) {
//                dmg_index = 8;
//            } else if (ratio < 7) {
//                dmg_index = 9;
//            } else if (ratio < 8) {
//                dmg_index = 10;
//            } else {
//                dmg_index = 11;
//            }
//        }
//
//        dmg = damage[random.nextInt(10)][dmg_index];
//
//        return dmg;
//
//    }
//
//    /**
//     * Handles one round of attacks by an attacker stack against a defender
//     * stack.
//     *
//     * @param attacker the attacking stack
//     * @param defender the defending stack
//     * @param damage not used.
//     * @param atk_type type of attack.
//     */
//    public void doAttack(List<Unit> attacker, List<Unit> defender, int[] damage, int atk_type) {
//
//        if (attacker.isEmpty() || defender.isEmpty()) {
//            return;
//        }
//
//        int count = attacker.size(); // > defender.size() ? attacker.size() : defender.size();
////        for (int i = 0; i < count; i++) {
////            damage[i] = 0;
////        }
//        ListIterator<Unit> atk_it = attacker.listIterator();
//        ListIterator<Unit> def_it = defender.listIterator();
//        Unit atk = atk_it.next();
//        Unit def = def_it.next();
//        int def_indx = 0;
//        for (int i = 0; i < count; i++) {
//
//            int to_hit = getAtkAcc(atk_type, atk) - def.type_data.ag;
//            if (random.nextInt(20) + to_hit > 9) {
////                damage[def_indx] += getDam(atk, def, atk_type);
//                def.health_tmp -= getDam(atk, def, atk_type);
//            }
////            if (!atk_it.hasNext()) {
////                atk_it = attacker.listIterator();
////            }
//            if (!atk_it.hasNext()) {
//                return;
//            }
//            atk = atk_it.next();
//            if (!def_it.hasNext()) {
//                def_it = defender.listIterator();
//                def_indx = -1;
//            }
//            def = def_it.next();
//            def_indx++;
//        }
//
//    }
//
//    public void dropDead(List<Unit> stack) {
//        ListIterator<Unit> iter = stack.listIterator();
//        Unit unit = null;
//        while (iter.hasNext()) {
//            unit = iter.next();
//            if (unit.health_tmp <= 0) {
//                unit.health = 0;
//                iter.remove();
//            }
//        }
//    }
//
//    public void checkRout(List<Unit> stack) {
//        for (Unit unit : stack) {
//            if (unit.type_data.move_pts > 0 && unit.health_tmp < unit.health) {
//                if (random.nextInt(100) + 1 > unit.loyalty) {
//                    unit.routed = true;
//                    if (combat_type.equals(C.GROUND_COMBAT)) {
//                        unit.move_points = 0;
//                    }
//                }
//            }
//        }
//    }
//
//    public boolean checkRoutedKilled(List<Unit> stack) {
//        boolean rv = true;
//        for (Unit unit : stack) {
//            if (unit.health > 0 && !unit.routed) {
//                rv = false;
//                break;
//            }
//        }
//        return rv;
//    }
//
//    public void removeDead(List<Unit> stack) {
//        for (ListIterator<Unit> it = stack.listIterator(); it.hasNext();) {
//            Unit unit = it.next();
//            // check for dead fighters aboard carriers
//            if (!unit.cargo_list.isEmpty()) {
//                for (ListIterator<Unit> it1 = unit.cargo_list.listIterator(); it1.hasNext();) {
//                    Unit unit1 = it1.next();
//                    if (unit1.health <= 0) {
//                        it1.remove();
//                    }
//
//                }
//
//            }
//            if (unit.health <= 0) {
//                if (!unit.cargo_list.isEmpty()) {
//                    for (ListIterator<Unit> it1 = unit.cargo_list.listIterator(); it1.hasNext();) {
//                        Unit unit1 = it1.next();
//                        unit1.health = 0;
//                        it1.remove();
//                    }
//                }
//                it.remove();
//            } else if (unit.health > unit.health_tmp) {
//                unit.health = unit.health_tmp;
//            }
//        }
//    }
//
//    public boolean isSkip(Hex[] neighbours, int r_pos, Unit unit_b, int tile_set) {
//        boolean skip = false;
//        Hex hex = neighbours[r_pos];
//        if (hex == null) {
//            skip = true;
//        } else {
//            boolean[] terr_types = hex.getTerrain();
//            double move_cost = 1;
//            for (int k = 0; k < terr_types.length; k++) {
//                if (terr_types[k] == true) {
//                    move_cost *= terr_cost[k][tile_set][unit_b.type_data.move_type.ordinal()];
//
//                }
//
//            }
//            System.out.println("move_cost = " + move_cost);
//
//            int stack_size = hex.getStack().size();
//            if (move_cost == 0) {
//                skip = true;
//                System.out.println("move_cost");
//            } else if (stack_size >= 20) {
//                skip = true;
//                System.out.println("stack_size");
//            } else if (stack_size > 0) {
//                if (unit_b.owner != hex.getStack().get(0).owner) {
//                    skip = true;
//                    System.out.println("owner");
//                }
//            }
//        }
//
//        return skip;
//    }
//
//    public Unit moveRouted(ListIterator<Unit> it, Hex[] neighbours,
//            int r_pos, List<Unit> stack_b, Unit unit) {
//
//        Hex hex = neighbours[r_pos];
//        hex.getStack().add(unit);
//        stack_b.remove(unit);
//        setUnitCoords(false, current_planet, hex.getX(), hex.getY(), unit);
//        if (it.hasNext()) {
//            return it.next();
//        } else {
//            return null;
//        }
//    }
//
//    /**
//     * Retreats routed units. Tries to retreat units to hexes surrounding
//     * defender hex. Starts at hex opposite the attacker hex and works toward
//     * attacker hex skipping out of map hexes, full hexes and enemy occupied
//     * hexes.
//     *
//     * @param stack_a attacking stack.
//     * @param stack_b defending stack.
//     */
//    public void retreatRouted(List<Unit> stack_a, List<Unit> stack_b) {
//        List<Unit> routed = new LinkedList<>();
//        for (Unit unit : stack_b) {
//            if (unit.routed && unit.health > 0) {
//                routed.add(unit);
//            }
//        }
//        if (routed.isEmpty()) {
//            return;
//        }
//        Unit unit_a = stack_a.get(0);
//        PlanetGrid pg = getPlanetGrid(unit_a.p_idx);
//        Hex hex_a = pg.getHex(unit_a.x, unit_a.y);
//        Unit unit_b = stack_b.get(0);
//        Hex hex_b = pg.getHex(unit_b.x, unit_b.y);
//        Hex[] neighbours = hex_b.getNeighbours();
//        int tile_set = planets.get(unit_a.p_idx).tile_set_type;
//        int a_pos = -1;
//        for (int i = 0; i < neighbours.length; i++) {
//            if (neighbours[i].equals(hex_a)) {
//                a_pos = i;
//                break;
//            }
//
//        }
//        int r_pos = a_pos + 3;
//        if (r_pos > 5) {
//            r_pos -= 6;
//        }
//        int r_pos_stored = r_pos;
//        boolean skip = false;
//        int state = 0;
//        ListIterator<Unit> it = routed.listIterator();
//        Unit unit = it.next();
//        while (unit != null) {
//            switch (state) {
//                case 0:
//                    skip = isSkip(neighbours, r_pos, unit, tile_set);
//                    if (skip) {
//                        r_pos -= 1;
//                        if (r_pos < 0) {
//                            r_pos += 6;
//                        }
//                        state = 1;
//                    } else {
//                        unit = moveRouted(it, neighbours, r_pos, stack_b, unit);
//                    }
//                    break;
//                case 1:
//                    skip = isSkip(neighbours, r_pos, unit, tile_set);
//                    if (skip) {
//                        r_pos += 2;
//                        if (r_pos > 5) {
//                            r_pos -= 6;
//                        }
//                        state = 2;
//                    } else {
//                        unit = moveRouted(it, neighbours, r_pos, stack_b, unit);
//                        state = 0;
//                        r_pos = r_pos_stored;
//                    }
//                    break;
//                case 2:
//                    skip = isSkip(neighbours, r_pos, unit, tile_set);
//                    if (skip) {
//                        r_pos -= 3;
//                        if (r_pos < 0) {
//                            r_pos += 6;
//                        }
//                        state = 3;
//                    } else {
//                        unit = moveRouted(it, neighbours, r_pos, stack_b, unit);
//                        state = 0;
//                        r_pos = r_pos_stored;
//                    }
//                    break;
//                case 3:
//                    skip = isSkip(neighbours, r_pos, unit, tile_set);
//                    if (skip) {
//                        r_pos += 4;
//                        if (r_pos > 5) {
//                            r_pos -= 6;
//                        }
//                        state = 4;
//                    } else {
//                        unit = moveRouted(it, neighbours, r_pos, stack_b, unit);
//                        state = 0;
//                        r_pos = r_pos_stored;
//                    }
//                    break;
//                case 4:
//                    skip = isSkip(neighbours, r_pos, unit, tile_set);
//                    if (skip) {
//                        state = 5;
//                    } else {
//                        unit = moveRouted(it, neighbours, r_pos, stack_b, unit);
//                        state = 0;
//                        r_pos = r_pos_stored;
//                    }
//                    break;
//                case 5:
//
//                    unit.health = 0;
//                    unit.health_tmp = 0;
//                    if (it.hasNext()) {
//                        unit = it.next();
//                    } else {
//                        unit = null;
//                    }
//                    state = 0;
//                    r_pos = r_pos_stored;
//                    break;
//                default:
//                    throw new AssertionError();
//            }
//
//        }
//
//    }
//
//    public void cancelRout(List<Unit> attacker, List<Unit> defender) {
//        StackIterator iterator = new StackIterator(attacker);
//        Unit e = iterator.next();
//        while (e != null) {
//            e.routed = false;
//            System.out.println("e.routed = " + e.routed);
//            e = iterator.next();
//        }
//
//        iterator = new StackIterator(defender);
//        e = iterator.next();
//        while (e != null) {
//            e.routed = false;
//            e = iterator.next();
//        }
//    }
//
//    public void assignDamage(List<Unit> attacker, List<Unit> defender) {
//        for (Unit unit : defender) {
//            unit.health = unit.health_tmp;
//        }
//        for (Unit unit : attacker) {
//            unit.health = unit.health_tmp;
//        }
//    }
//
////    public void resolveBattleInit()
//    public void selectGroundFighters() {
//
//        List<Unit> stack_a = getSelectedStack();
//        List<Unit> stack_b = path.get(1).getStack();
//
//        List<Unit> attacker = new LinkedList<>();
//        for (Unit unit : stack_a) {
//            if (unit.selected && unit.type_data.non_combat == 0
//                    && unit.type_data.move_type != C.MoveType.JUMP
//                    && unit.type_data.move_type != C.MoveType.SPACE) {
//                attacker.add(unit);
//                unit.health_tmp = unit.health;
//            }
//        }
//        combat_stack_a = attacker;
//        Util.sortRank(combat_stack_a);
//        subMovePoints(attacker);
//
//        List<Unit> defender = new LinkedList<>();
//        for (Unit unit : stack_b) {
//            if (unit.type_data.non_combat == 0
//                    && unit.type_data.move_type != C.MoveType.JUMP
//                    && unit.type_data.move_type != C.MoveType.SPACE) {
//                defender.add(unit);
//                unit.health_tmp = unit.health;
//            }
//        }
//        combat_stack_b = defender;
//        Util.sortRank(combat_stack_b);
//    }
//
//    public void selectSpaceFightersB(Unit unit, List<Unit> combat, boolean attacker) {
//
//        combat.add(unit);
//        unit.health_tmp = unit.health;
//
//        if (unit.type == 16) {
//            for (Unit unit1 : unit.cargo_list) {
//                switch (unit1.type) {
//                    case 17:
//                    case 18:
//                        combat.add(unit1);
//                        unit1.health_tmp = unit1.health;
//                        if (attacker) {
//                            unit1.move_points++;
//                        }
//                        break;
//                    default:
//                }
//            }
//        }
//
//    }
//
//    /**
//     * Select fighters for a space battle. Selected combat units are selected
//     * for attackers, all units are selected for defenders. Additionally using
// * {@link #selectSpaceFightersB(galaxyreader.Unit, java.util.List, boolean) }
//     * fighters and bombers aboard carriers are selected.
//     */
//    public void selectSpaceFighters() {
//
//        List<Unit> stack_a = getSelectedStack();
//        List<Unit> stack_b = null;
//        Point p = getSelectedPoint();
//        Square[][] grid = galaxy_grid.getGalaxyGrid();
//        Planet planet = grid[p.x][p.y].parent_planet;
//        stack_b = planet.space_stacks[attacked_faction];
//
//        List<Unit> attacker = new LinkedList<>();
//        for (Unit unit : stack_a) {
//            // if you change this change checkMoveLeftSpace also
//            if (unit.selected && unit.type_data.non_combat == 0) {
//                selectSpaceFightersB(unit, attacker, true);
//            }
//        }
//        combat_stack_a = attacker;
//        Util.sortRank(combat_stack_a);
//        subMovePointsSpace(attacker);
//
//        List<Unit> defender = new LinkedList<>();
//        for (Unit unit : stack_b) {
//            selectSpaceFightersB(unit, defender, false);
//        }
//
//        combat_stack_b = defender;
//        Util.sortRank(combat_stack_b);
//    }
    public void resolveGroundBattleInit(String combat_type, int defender_owner) {
        battle.perBattleInit(path, current_planet);
        battle.resolveGroundBattleInit(combat_type, defender_owner);
//        this.combat_type = combat_type;
//        this.attacked_faction = defender_owner;
//
//        switch (combat_type) {
//            case C.GROUND_COMBAT:
//                selectGroundFighters();
//                break;
//            case C.SPACE_COMBAT:
//                selectSpaceFighters();
//                break;
//            default:
//                throw new AssertionError();
//        }

    }

    public void resolveGroundBattleFight() {
        battle.resolveGroundBattleFight();
//        doCombat(combat_stack_a, combat_stack_b);
//        assignDamage(combat_stack_a, combat_stack_b);

    }

    public void resolveGroundBattleFinalize() {
        battle.resolveGroundBattleFinalize();
//        List<Unit> stack_a = getSelectedStack();
//        List<Unit> stack_b = null;
//        if (combat_type.equals(C.GROUND_COMBAT)) {
//            stack_b = path.get(1).getStack();
//            retreatRouted(stack_a, stack_b);
//        } else if (combat_type.equals(C.SPACE_COMBAT)) {
//            Point p = getSelectedPoint();
//            Square[][] grid = galaxy_grid.getGalaxyGrid();
//            Planet planet = grid[p.x][p.y].parent_planet;
//            stack_b = planet.space_stacks[attacked_faction];
//            cancelRout(stack_a, stack_b);
//        }
//        removeDead(stack_a);
//        removeDead(stack_b);
//
//        removeDead(units);
//        removeDead(unmoved_units);
//        combat_stack_a = null;
//        combat_stack_b = null;
//        combat_type = null;
//        attacked_faction = -2;
    }

//    /**
//     * Handles a battle between an attacking and a defending stack. During each
//     * iteration through combat phases, for each phase, 1. selects offensive and
//     * defensive units for both sides, 2. shuffles offensive units, 3. conducts
//     * attacks in the phase while dropping dead units, 4. checks for rout.
//     *
//     * @param attacker attacking stack.
//     * @param defender defending stack.
//     */
//    public void doCombat(List<Unit> attacker, List<Unit> defender) {
//
//        int[] defender_damage = new int[C.STACK_SIZE];
//        List<Unit> defender_defence = new LinkedList<>();
//        List<Unit> defender_offence = new LinkedList<>();
//
//        int[] attacker_damage = new int[C.STACK_SIZE];
//        List<Unit> attacker_offence = new LinkedList<>();
//        List<Unit> attacker_defence = new LinkedList<>();
//
//        int combat_loop_iter = -1;
//        int[] combat_phases = null;
//
//        switch (combat_type) {
//            case C.GROUND_COMBAT:
//                combat_loop_iter = C.COMBAT_LOOP_ITER;
//                combat_phases = C.GROUND_COMBAT_PHASES;
//                break;
//            case C.SPACE_COMBAT:
//                combat_loop_iter = 1;
//                combat_phases = C.SPACE_COMBAT_PHASES;
//                break;
//            default:
//                throw new AssertionError();
//        }
//        fight:
//        for (int k = 0; k < combat_loop_iter; k++) {
//            for (int i = 0; i < combat_phases.length; i++) {
//                int phase = combat_phases[i];
//                attacker_offence.clear();
//                attacker_defence.clear();
//                chooseOffence(attacker, attacker_offence, phase);
//                chooseDefence(attacker, attacker_defence, phase);
////                System.out.println("attacker_offence = " + attacker_offence);
////                System.out.println("attacker_defence = " + attacker_defence);
//                defender_defence.clear();
//                defender_offence.clear();
//                chooseDefence(defender, defender_defence, phase);
//                chooseOffence(defender, defender_offence, phase);
////                System.out.println("defender_defence = " + defender_defence);
////                System.out.println("defender_offence = " + defender_offence);
//                Collections.shuffle(attacker_offence, random);
//                Collections.shuffle(defender_offence, random);
//                for (int j = 0; j < C.NR_ATTACKS[phase]; j++) {
//                    doAttack(attacker_offence, defender_defence, defender_damage, phase);
//                    doAttack(defender_offence, attacker_defence, attacker_damage, phase);
//                    dropDead(attacker_offence);
//                    dropDead(defender_defence);
//                    dropDead(attacker_defence);
//                    dropDead(defender_offence);
//                }
//                checkRout(attacker);
//                checkRout(defender);
//                if (checkRoutedKilled(attacker) || checkRoutedKilled(defender)) {
//                    break fight;
//                }
//
//            }
//        }
//
//    }
    public String getCombatType() {
        return battle.getCombatType();
//        return combat_type;
    }

    public boolean isEnemy() {
        boolean rv = false;

        if (path.size() == 2) {
            List<Unit> stack = path.get(1).getStack();
            if (!stack.isEmpty() && stack.get(0).owner != turn) {
                rv = true;
            }
        }

        return rv;
    }

    public boolean isCombat() {
        boolean rv = false;

        if (path.size() == 2) {
            List<Unit> stack = path.get(1).getStack();
            for (Unit unit : stack) {
                if (unit.owner != turn && unit.type_data.non_combat == 0 && !unit.routed) {
                    rv = true;
                    break;
                }

            }
        }

        return rv;
    }

    public boolean isCapture() {

        boolean captee = false;
        boolean captor = false;

        if (path.size() == 2) {
            List<Unit> stack = path.get(1).getStack();
            if (!stack.isEmpty()) {
                captee = true;
            }
            for (Unit unit : stack) {
                if (unit.owner == turn || (unit.type_data.non_combat == 0 && !unit.routed
                        && unit.type_data.move_type != C.MoveType.SPACE
                        && unit.type_data.move_type != C.MoveType.JUMP)) {
                    captee = false;
                    break;
                }

            }

            List<Unit> stack2 = path.get(0).getStack();
            for (Unit unit : stack2) {
                if (unit.type_data.non_combat == 0 && (unit.type_data.move_type != C.MoveType.AIR
                        && unit.type_data.move_type != C.MoveType.SPACE
                        && unit.type_data.move_type != C.MoveType.JUMP)) {
                    captor = true;
                    break;
                }
            }
        }

        return captor && captee;
    }

    public boolean checkMoveLeftSpace(List<Unit> stack) {
        boolean rv = true;
        for (Unit unit : stack) {
            // if you change this change selectSpaceFighters also
            if (unit.selected && unit.type_data.non_combat == 0
                    && unit.move_points < 1) {
                rv = false;
                break;
            }
        }
        return rv;
    }

    public void capture() {

        List<Unit> stack = path.get(1).getStack();

        StackIterator iter = new StackIterator(stack);

        Unit u = iter.next();
        while (u != null) {
            changeOwnerOfUnit(turn, u);
            u.spotted[turn] = true;
            u = iter.next();
        }
    }

    public int getTurn() {
        return turn;
    }

    public Random getRandom() {
        return random;
    }

    public EfsIni getEfs_ini() {
        return efs_ini;
    }

    public List<Planet> getPlanets() {
        return planets;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public List<Structure> getStructures() {
        return structures;
    }

    public List<Unit> getSelectedStack() {
        List<Unit> stack = null;
        Point p = getSelectedPoint();
        if (p == null) {
            return null;
        }
        int faction = getSelectedFaction();
        if (faction == -1) {
            stack = getPlanetGrid(getCurrentPlanetNr()).getHex(p.x, p.y).getStack();
        } else {
            Square[][] galaxy_grid = getGalaxyMap().getGalaxyGrid();
            stack = galaxy_grid[p.x][p.y].parent_planet.space_stacks[faction];
            System.out.println("stack = " + stack);
        }
        return stack;
    }

//    public void setNextUnmovedUnit() {
//        
//    }
    public void setFactionPlayer(int faction, boolean human_player) {
        human_ctrl[faction] = human_player;
    }

    public boolean humanPlayerPresent() {
        boolean rv = false;

        for (int i = 0; i < human_ctrl.length; i++) {
            if (human_ctrl[i]) {
                rv = true;
                break;
            }

        }

        return rv;
    }

    public boolean[] getHumanControl() {
        return human_ctrl;
    }

    public int getYear() {
        return year;
    }

    public void beginGame() {
        advanceTurn();
        while (!human_ctrl[turn]) {
            advanceTurn();
        }

    }

    public void endTurn() {
        factions[turn].deleteOldMessages();
        advanceTurn();
        while (!human_ctrl[turn]) {
            advanceTurn();
        }
    }

    public void advanceTurn() {
        if (turn >= 13) {
            turn = 0;
            year++;
        } else {
            turn++;
        }

        economy.updateEconomy(turn);    //RSW

//        factions[turn].deleteOldMessages(year);
        setFactionCities();
        doResearch();
        buildUnits();

        resetUnmovedUnits();
        resetMovePoints();
        setMaxSpotRange();
        cargo_pods = Util.getCargoPods(units, this);
    }

    public List<Unit> getCargoPods() {
        return cargo_pods;
    }

    /**
     * Will try to build units. In building city or if it has full stack then
     * will try surrounding hexes, if there are no enemy units or cities or full
     * stack or impassable terrain will build unit there. If it fails to build
     * it will send city full message to owning faction.
     */
    public void buildUnits() {
        for (Structure city : faction_cities) {
            if (city.build_queue.isEmpty()) {
                continue;
            }
            Unit unit = null;
            if (!city.on_hold_no_res) {
                if (city.turns_left == 1) {
                    System.out.println("Here");
                    int[] u_t = city.build_queue.getFirst();
                    C.MoveType move = unit_types[u_t[0]][u_t[1]].move_type;
                    Hex hex = findRoom(city, move);
                    if (hex != null) {
                        city.buildUnits(unit_types, this, hex);

                    } else {
                        factions[turn].addMessage(new Message(null, C.Msg.CITY_FULL, year, city));
                    }
                } else {
                    city.buildUnits(unit_types, this, null);
                }
            } else {
                city.tryToStartBuild(city.build_queue.getFirst(), unit_types, this);
            }
        }
    }

    /**
     * Tries to find room for one unit in a city or surrounding hexes.
     *
     * @param city
     * @param move move type of unit
     * @return Hex where there is room or null if no room found;
     */
    public Hex findRoom(Structure city, C.MoveType move) {
        boolean found = false;
        Hex hex = planets.get(city.p_idx).planet_grid.getHex(city.x, city.y);
        if (Util.stackSize(hex.getStack()) < C.STACK_SIZE) {
            found = true;
        }

        int tile_set = planets.get(city.p_idx).tile_set_type;
        Hex[] neighbors = hex.getNeighbours();
        test:
        for (int i = 0; i < neighbors.length && !found; i++) {
            hex = neighbors[i];
            if (hex == null) {
                continue;
            }
            Structure city_h = hex.getStructure();
            if (city_h != null) {
                if (city_h.owner != turn) {
                    continue;
                }
            }
            List<Unit> stack = hex.getStack();
            if (!stack.isEmpty()) {
                if (stack.get(0).owner != turn) {
                    continue;
                }
            }
            if (Util.stackSize(stack) < C.STACK_SIZE) {
                if (city_h == null) {
                    boolean[] terrain = hex.getTerrain();
                    for (int j = 0; j < terrain.length; j++) {
                        if (terrain[j] && terr_cost[j][tile_set][move.ordinal()] == 0) {
                            continue test;
                        }
                    }
                }
                found = true;
            }

        }
        if (!found) {
            hex = null;
        }
        return hex;
    }

    public HexProc getHexProc() {
        return hex_proc;
    }

    public Faction getFaction(int faction) {
        return factions[faction];
    }

    public void setFactionCities() {
        faction_cities.clear();
        for (Structure s : structures) {
            if (s.owner == turn) {
                faction_cities.add(s);
            }
        }
    }

    public List<Unit> getUnmovedUnits() {
        return unmoved_units;
    }

    public void resetUnmovedUnits() {
        unmoved_units.clear();
        for (Unit u : units) {
            if (u.owner == turn) {
//                System.out.println("u.owner = " + u.owner);
                unmoved_units.add(u);
                u.routed = false;
            }
        }

        Collections.sort(unmoved_units, new Comparator<Unit>() {
            public int compare(Unit o1, Unit o2) {
                int x = o1.x - o2.x;
                if (x != 0) {
                    return x;
                } else {
                    return o1.y - o2.y;
                }

            }
        });

        Collections.sort(unmoved_units, new Comparator<Unit>() {
            public int compare(Unit o1, Unit o2) {
                return o1.p_idx - o2.p_idx;
            }
        });

    }

    public void setJumpRoutes() {
        for (JumpGate jg : jump_gates) {
            Square[][] galaxy_map = galaxy_grid.getGalaxyGrid();
            Planet p1 = galaxy_map[jg.getX1()][jg.getY1()].planet;
            Planet p2 = galaxy_map[jg.getX2()][jg.getY2()].planet;
            if (p1 != null && p2 != null) {
                p1.jump_routes.add(jg);
                p2.jump_routes.add(jg);
            }

        }
    }

    public void printMoveCost() {
        for (int i = 0; i < terr_cost.length; i++) {
            for (int j = 0; j < terr_cost[i].length; j++) {
                for (int k = 0; k < terr_cost[i][j].length; k++) {
                    System.out.print(" " + terr_cost[i][j][k]);

                }
                System.out.println("");
            }
            System.out.println("");
        }
    }

    public void resetMovePoints() {
        for (Unit e : units) {
            if (e.owner == turn) {
                e.move_points = unit_types[e.type][e.t_lvl].move_pts;
            }
        }
    }

    public void setUnitTypeData() {
        for (Unit e : units) {
            e.type_data = unit_types[e.type][e.t_lvl];
        }

    }

    public void setMoveType() {
        for (Unit e : units) {
            e.move_type = unit_types[e.type][e.t_lvl].move_type;
        }
    }

    public void placeUnits() {

        int y;

        int out_of_map_units = 0;
        Planet planet;
        ListIterator<Unit> iterator = units.listIterator();
//        for (Unit e : units) {
        while (iterator.hasNext()) {
            Unit e = iterator.next();
            planet = planets.get(e.p_idx);
            if (e.in_space) {
                planet.placeUnit(e);
                y = e.y;
            } else { // column numbering is special
                if (e.x % 2 == 0) {
                    y = (e.y - 1) / 2;
                } else {
                    y = e.y / 2;
                }
                if (e.x % 2 == 0 && y >= 31) {

                    System.out.println("Out of map unit number " + out_of_map_units++);
                    iterator.remove();
                    System.out.println("Owner " + e.owner);
                    System.out.println("Planet " + planets.get(e.p_idx).name);
                } else {
//                    System.out.println("(x,y): " + e.x + "," + y);
                    planet.planet_grid.getHex(e.x, y).placeUnit(e);

                }
            }
            e.y = y;
            if (e.in_space) {
                //debug code
//                if (planet.x != e.x || planet.y != e.y) {
//                    System.out.println("Off square space unit");
//                    System.out.print("planet = " + planet);
//                    System.out.println("e.owner = " + e.owner);
//                    System.out.print("planet.x = " + planet.x + "planet.y = " + planet.y);
//                    System.out.println("e.x = " + e.x + "e.y = " + e.y);
//                }

//                Point p = convertSpaceUnit(new Point(e.x, e.y), e.owner);
                e.x = planet.x;
                e.y = planet.y;
            }
        }

    }

    public void placeStructures() {

        int y;

        int out_of_map_units = 0;

        ListIterator<Structure> iterator = structures.listIterator();
//        for (Unit e : units) {
        while (iterator.hasNext()) {
            Structure e = iterator.next();

            if (e.type < 32) {
                if (e.x % 2 == 0) {
                    y = (e.y - 1) / 2;
                } else {
                    y = e.y / 2;
                }
                if ((e.x % 2 == 0 && y >= 31) || (e.x % 2 == 1 && y >= 32) || y < 0) {

                    System.out.println("Out of map structure number " + out_of_map_units++);
                    iterator.remove();
                    System.out.println("Owner " + e.owner);
                    System.out.println("Planet " + planets.get(e.p_idx).name);
                    System.out.println("Hex(x,y): " + e.x + "," + y);
                } else {
//                    System.out.println("(x,y): " + e.x + "," + y);
                    if (e.type < 26) {
                        planets.get(e.p_idx).planet_grid.getHex(e.x, y).placeStructure(e);
                    } else {
                        planets.get(e.p_idx).planet_grid.getHex(e.x, y).placeResource(e);
                    }
                }
                e.y = y;
            }
        }

    }

    public void subMovePoints(List<Unit> selected) {
        Hex destination = getPath().get(1);
//        int current_planet = getCurrentPlanetNr();
//        PlanetGrid planet_grid = getPlanetGrid(current_planet);
//        Point sel = getSelectedPoint();
//        List<Unit> stack = planet_grid.getHex(sel.x, sel.y).getStack();
//        System.out.println("stack = " + stack);
//        List<Unit> selected = new LinkedList<>();
//
//        for (Unit unit : stack) {
//            if (unit.selected) {
//                selected.add(unit);
//                System.out.println("unit = " + unit);
//            }
//        }

        for (Unit e : selected) {
            int move_cost = destination.getMoveCost(e.move_type.ordinal());
            int max_move = e.type_data.move_pts;
            if (move_cost > max_move) {
                move_cost = max_move;
            }
            e.move_points -= move_cost;
            System.out.println("move_cost = " + move_cost);
        }

    }

    public void subMovePointsSpace(List<Unit> selected) {
        for (Unit unit : selected) {
            unit.move_points--;
        }
    }

    public void unSpot(List<Unit> stack) {
        for (Unit unit : stack) {
            for (int i = 0; i < unit.spotted.length; i++) {
                if (i != unit.owner) {
                    unit.spotted[i] = false;
                }
            }
        }
    }

    public void spotSpace(Planet planet, List<Unit> stack, int faction) {
        int spotting_a = 0;
        for (Unit unit : stack) {
            if (unit.type_data.spot > spotting_a) {
                spotting_a = unit.type_data.spot;
            }
        }
        List<Unit>[] stacks = planet.space_stacks;
        for (int i = 0; i < stacks.length; i++) {
            List<Unit> stack_b = stacks[i];
            if (!stack_b.isEmpty()) {
                int spotting_b = 0;
                for (Unit unit : stack_b) {
                    if (unit.type_data.spot > spotting_b) {
                        spotting_b = unit.type_data.spot;
                    }
                }
                spotSpaceStack(stack_b, spotting_a, faction);
                spotSpaceStack(stack, spotting_b, stack_b.get(0).owner);
            }
        }
        planet.spotted[faction] = true;
    }

    public void spotSpaceStack(List<Unit> stack, int spotting, int faction) {
        for (Unit unit : stack) {
            if (spotting >= unit.type_data.camo) {
                unit.spotted[faction] = true;
            }
        }
    }

    public boolean moveStack() {
        boolean rv = false;
        Hex hex = path.getFirst();
        Hex hex2 = path.get(1);
        List<Unit> stack = hex.getStack();
        List<Unit> stack2 = hex2.getStack();
        List<Unit> selected = new LinkedList<>();
        for (Unit unit : stack) {
            if (unit.selected) {
                selected.add(unit);
            }
        }
        Util.unSelectAll(stack2);

        boolean own_units = true;
        if (!stack2.isEmpty() && selected.get(0).owner != stack2.get(0).owner) {
            own_units = false;
        }
        if (Util.stackSize(selected) + Util.stackSize(stack2) <= 20 && own_units) {
            subMovePoints(selected);
            hex2.addStack(selected);
            hex.minusStack(selected);
            setUnitCoords(false, current_planet, hex2.getX(), hex2.getY(), selected);

            path.removeFirst();
            setSelectedPoint(new Point(hex2.getX(), hex2.getY()), -1);
            unSpot(selected);
            hex_proc.spotProc(hex2, selected);
            rv = true;
        }
        return rv;
    }

    public boolean moveSpaceStack(Point p) {
        boolean rv = false;
        Square[][] galaxy_map = getGalaxyMap().getGalaxyGrid();
        int x1 = selected_point.x;
        int y1 = selected_point.y;
        Planet source = galaxy_map[x1][y1].parent_planet;
        Planet destination = galaxy_map[p.x][p.y].planet;
        List<Unit> stack = source.space_stacks[selected_faction];
        List<Unit> stack2 = destination.space_stacks[selected_faction];
        List<Unit> selected = new LinkedList<>();
        for (Unit unit : stack) {
            if (unit.selected) {
                selected.add(unit);
            }
        }
        Util.unSelectAll(stack2);

        if (Util.stackSize(selected) + Util.stackSize(stack2) <= 20) {
            for (Unit unit : selected) {
                unit.move_points = 0;
            }
            destination.addStack(selected, selected_faction);
            source.minusStack(selected, selected_faction);
            unSpot(selected);
            spotSpace(destination, selected, selected_faction);
            setUnitCoords(true, destination.index, p.x, p.y, selected);
            rv = true;
        }

        return rv;
    }

    public boolean launchStack() {
        boolean rv = false;
        Point q = getSelectedPoint();
        PlanetGrid planet_grid = getPlanetGrid(getCurrentPlanetNr());
        Hex target_hex = planet_grid.getHex(q.x, q.y);
        List<Unit> stack = target_hex.getStack();
        List<Unit> selected = new LinkedList<>();
        for (Unit unit : stack) {
            if (unit.selected) {
                selected.add(unit);
            }
        }
        int faction = selected.get(0).owner;
//        System.out.println("faction = " + faction);
        Planet planet = planets.get(getCurrentPlanetNr());
        List<Unit> target_stack = planet.space_stacks[faction];
        Util.unSelectAll(target_stack);

        if (Util.stackSize(selected) + Util.stackSize(target_stack) <= 20) {
            subMovePointsSpace(selected);
            planet.addStack(selected, faction);
            target_hex.minusStack(selected);
            unSpot(selected);
            spotSpace(planet, selected, faction);
            setUnitCoords(true, planet.index, planet.x, planet.y, selected);
            //need to set it like this :(
            setSelectedPointFaction(new Point(planet.x, planet.y), faction, null, null);
            setSelectedPoint(new Point(planet.x, planet.y), faction);
            setSelectedFaction(faction);
//            System.out.println("selected_point = " + selected_point);
//            System.out.println("faction = " + faction);
            rv = true;
        }
        return rv;
    }

    public boolean landStack(Point p) {
        boolean rv = false;
        Square[][] galaxy_map = getGalaxyMap().getGalaxyGrid();
        int x1 = selected_point.x;
        int y1 = selected_point.y;
        Planet planet = galaxy_map[x1][y1].parent_planet;
        List<Unit> stack = planet.space_stacks[selected_faction];

        Hex target_hex = planet.planet_grid.getHex(p.x, p.y);
        List<Unit> stack2 = target_hex.getStack();
        List<Unit> selected = new LinkedList<>();
        for (Unit unit : stack) {
            if (unit.selected) {
                selected.add(unit);
            }
        }
        Util.unSelectAll(stack2);

        if (Util.stackSize(selected) + Util.stackSize(stack2) <= 20) {
            subMovePointsSpace(selected);
            target_hex.addStack(selected);
            planet.minusStack(selected, selected_faction);
            setUnitCoords(false, planet.index, p.x, p.y, selected);
            setSelectedPointFaction(p, -1, null, null);
            unSpot(selected);
            hex_proc.spotProc(target_hex, selected);
            rv = true;
        }
        return rv;
    }

    public void setUnitCoords(boolean in_space, int p_idx, int x, int y, Unit e) {

        relocateUnit(in_space, p_idx, x, y, e);

        for (Unit u : e.cargo_list) {
            relocateUnit(in_space, p_idx, x, y, u);
        }

    }

    public void setUnitCoords(boolean in_space, int p_idx, int x, int y, List<Unit> selected) {
        boolean is_cargo_listing = false;
        Iterator<Unit> iterator = selected.listIterator();
        Iterator<Unit> cargo_it = null;

        Unit e = iterator.next();
        for (int i = 0; i < C.STACK_SIZE; i++) {

            relocateUnit(in_space, p_idx, x, y, e);

            if (is_cargo_listing) {
                e = cargo_it.next();
                if (!cargo_it.hasNext()) {
                    cargo_it = null;
                    is_cargo_listing = false;
                }
            } else if (e.cargo_list.isEmpty()) {
                if (iterator.hasNext()) {
                    e = iterator.next();
                } else {
                    return;
                }
            } else {
                cargo_it = e.cargo_list.listIterator();
                e = cargo_it.next();
                if (cargo_it.hasNext()) {
                    is_cargo_listing = true;
                }
            }

        }
    }

    /**
     * Note for space stacks the selected point is complicated, it is one of the
     * squares around the parent planet. So when setting the selected point
     * using the planets coordinates one has to use setSelectedPoint(p, faction)
     * and setSelectedFaction(faction).
     *
     * @param p the value of p
     * @param faction the value of faction
     * @param path the value of path
     */
    public void setSelectedPointFaction(Point p, int faction, LinkedList<Hex> path, JumpGate jump_path) {
        setSelectedPoint(p, -1);
        setSelectedFaction(faction);
        setPath(path);
        setJumpPath(jump_path);
    }

    public void setJumpPath(JumpGate jg) {
        jump_path = jg;
    }

    public JumpGate getJumpPath() {
        return jump_path;
    }

    /**
     * For a space stack, converts Point coordinates from planet coordinates to
     * faction square coordinates around planet.
     *
     * @param p
     * @param faction
     * @return
     */
    public Point resolveSpaceStack(Point p, int faction) {
        Point q = new Point(p.x, p.y);
        if (faction > -1) {

            switch (faction) {

                case C.HOUSE1:
                    q.x += -1;
                    q.y += -1;
                    break;
                case C.HOUSE2:
                    q.x += 0;
                    q.y += -1;
                    break;
                case C.HOUSE3:
                    q.x += 1;
                    q.y += -1;
                    break;
                case C.HOUSE4:
                    q.x += -1;
                    q.y += 0;
                    break;
                case C.HOUSE5:
                    q.x += 1;
                    q.y += 0;
                    break;
                case C.LEAGUE:
                case C.THE_CHURCH:
                case C.SYMBIOT:
                    q.x += -1;
                    q.y += 1;
                    break;
                case C.VAU:
                case C.IMPERIAL:
                case C.FLEET:
                    q.x += 0;
                    q.y += 1;
                    break;
                case C.STIGMATA:
                case C.THE_SPY:
                case C.NEUTRAL:
                    q.x += 1;
                    q.y += 1;
                    break;
                default:
                    throw new AssertionError();
            }

        }
        return q;
    }

    public Point convertSpaceUnit(Point p, int faction) {
        if (faction > -1) {

            switch (faction) {

                case C.HOUSE1:
                    p.x += +1;
                    p.y += +1;
                    break;
                case C.HOUSE2:
                    p.x += 0;
                    p.y += +1;
                    break;
                case C.HOUSE3:
                    p.x += -1;
                    p.y += +1;
                    break;
                case C.HOUSE4:
                    p.x += +1;
                    p.y += 0;
                    break;
                case C.HOUSE5:
                    p.x += -1;
                    p.y += 0;
                    break;
                case C.LEAGUE:
                case C.THE_CHURCH:
                case C.SYMBIOT:
                    p.x += +1;
                    p.y += -1;
                    break;
                case C.VAU:
                case C.IMPERIAL:
                case C.FLEET:
                    p.x += 0;
                    p.y += -1;
                    break;
                case C.STIGMATA:
                case C.THE_SPY:
                case C.NEUTRAL:
                    p.x += -1;
                    p.y += -1;
                    break;
                default:
                    throw new AssertionError();
            }

        }
        return p;
    }

    /**
     * Set the selected point. For space the faction of the selected units must
     * be given also to determine the proper square next to the planet.
     *
     * @param p the selected point
     * @param faction for space, the faction of the selected units.
     */
    public void setSelectedPoint(Point p, int faction) {
        if (faction > -1) {

            switch (faction) {

                case C.HOUSE1:
                    p.x += -1;
                    p.y += -1;
                    break;
                case C.HOUSE2:
                    p.x += 0;
                    p.y += -1;
                    break;
                case C.HOUSE3:
                    p.x += 1;
                    p.y += -1;
                    break;
                case C.HOUSE4:
                    p.x += -1;
                    p.y += 0;
                    break;
                case C.HOUSE5:
                    p.x += 1;
                    p.y += 0;
                    break;
                case C.LEAGUE:
                case C.THE_CHURCH:
                case C.SYMBIOT:
                    p.x += -1;
                    p.y += 1;
                    break;
                case C.VAU:
                case C.IMPERIAL:
                case C.FLEET:
                    p.x += 0;
                    p.y += 1;
                    break;
                case C.STIGMATA:
                case C.THE_SPY:
                case C.NEUTRAL:
                    p.x += 1;
                    p.y += 1;
                    break;
                default:
                    throw new AssertionError();
            }

        }
        selected_point = p;
    }

    public Point getSelectedPoint() {
        return selected_point;
    }

    public StrBuild getStrBuild(int index) {
        return str_build[index];
    }

    public StrBuild[] getStrBuild() {
        return str_build;
    }

    public void setMapOrigin(Point p) {
        planet_map_origin = p;

    }

    public Galaxy getGalaxy() {
        return galaxy;
    }

    public GalaxyGrid getGalaxyMap() {
        return galaxy_grid;
    }

    public Point getMapOrigin() {
        return planet_map_origin;
    }

    public Point getSpaceMapOrigin() {
        return space_map_origin;

    }

    public void setSpaceMapOrigin(Point p) {
        space_map_origin = p;
    }

    public int[][] getCurrentMap() {

        return galaxy.getPlanetMap(current_planet);
    }

    public void setCurrentPlanetNr(int current_planet) {
        this.current_planet = current_planet;
    }

    public int getCurrentPlanetNr() {
        return current_planet;
    }

    public Planet getPlanet(int index) {
        return planets.get(index);
    }

    public PlanetGrid getPlanetGrid(int index) {
        return planets.get(index).planet_grid;
    }

    public static void main(String[] args) {
        PlanetGrid map = new PlanetGrid();

        if (map.test()) {
            System.out.println("Map ok.");
        } else {
            System.out.println("Map not ok.");
        }

    }

    /**
     * @return the selected_faction
     */
    public int getSelectedFaction() {
        return selected_faction;
    }

    /**
     * @param selected_faction the selected_faction to set
     */
    public void setSelectedFaction(int selected_faction) {
        this.selected_faction = selected_faction;
    }

    /**
     * @return the path
     */
    public LinkedList<Hex> getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(LinkedList<Hex> path) {
        this.path = path;
    }

    /**
     * @return the terr_cost
     */
    public double[][][] getTerrCost() {
        return terr_cost;
    }

    /**
     * @param terr_cost the terr_cost to set
     */
    public void setTerrCost(double[][][] terr_cost) {
        this.terr_cost = terr_cost;
    }

    /**
     * @return the resource types
     */
    public ResType[] getResTypes() {
        return res_types;
    }

    /**
     * @return the unit_types
     */
    public UnitType[][] getUnitTypes() {
        return unit_types;
    }

    /**
     * @param unit_types the unit_types to set
     */
    public void setUnitTypes(UnitType[][] unit_types) {
        this.unit_types = unit_types;
    }

    public void getAllResources() {
        HexIter iter = Util.getHexIter(this, getCurrentPlanetNr());
        Hex hex = iter.next();
        while (hex != null) {
            if (!hex.getTerrain(C.OCEAN) && hex.getStack().isEmpty() && hex.getStructure() == null) {
                for (int i = 0; i < C.RES_TYPES; i++) {
                    createUnitInHex(getCurrentPlanetNr(), hex.getX(), hex.getY(), getTurn(), C.CARGO_UNIT_TYPE, 0, i, 999);
                }
                break;
            }
            hex = iter.next();
        }
    }

    public void setMaxSpotRange() {
        int spotting = 0;
        for (Unit unit : units) {
            if (spotting < unit.type_data.spot) {
                spotting = unit.type_data.spot;
            }
        }
        hex_proc.setMaxSpotRange(Unit.spotRange(spotting));

    }

    public GameResources getGameResources() {
        return game_resources;
    }

//    public List<Structure> getStructures() {
//        return structures;
//    }
//    public int getMaxSpotRange() {
//        return max_spot_range;
//    }
    /**
     * Capture (or change ownership of) city. City ownership change must be done
     * thru this method to update production and consumption data.
     *
     * @param city
     * @param new_owner
     */
    public void captureCity(Structure city, int new_owner) {
        //subtract prod_cons for old owner
        economy.updateProdConsForCity(city, false);
        city.owner = new_owner;
        //add prod_cons for new owner
        economy.updateProdConsForCity(city, true);
    }

    /**
     * Creates a city. New cities must be created thru this method to update
     * production and consumption data.
     *
     * @param owner
     * @param p_idx
     * @param x
     * @param y
     * @return
     */
    public Structure createCity(int owner, int p_idx, int x, int y, int type, int health) {
        Structure city = new Structure(owner, type, p_idx, x, y, health);
        Hex hex = getHexFromPXY(p_idx, x, y);
        hex.placeStructure(city);
        structures.add(city);
        // update production/consumption data
        economy.updateProdConsForCity(city, true);
        return city;
    }

    /**
     * Adjust city health to new health. City health must be changed thru this
     * method to update production and consumption data.
     *
     * @param city
     * @param new_health
     */
    public void adjustCityHealth(Structure city, int new_health) {
        //subtract prod_cons for old health
        economy.updateProdConsForCity(city, false);
        city.health = new_health;
        //add prod_cons for new health
        economy.updateProdConsForCity(city, true);
    }

    /**
     * Adjust city loyalty to new loyalty. City loyalty must be changed thru
     * this method to update production and consumption data.
     *
     * @param city
     * @param new_loyalty
     */
    public void adjustCityLoyalty(Structure city, int new_loyalty) {
        //subtract subtract prod_cons for old loyalty
        economy.updateProdConsForCity(city, false);
        city.loyalty = new_loyalty;
        //add prod_cons for new loyalty
        economy.updateProdConsForCity(city, true);
    }

    /**
     * Make and place fresh unit from scratch, not from Galaxy file. Places unit
     * on a planet at specified coordinates. Units cannot be created in space
     * during game. If hex is already full, does nothing and returns null.
     *
     * @param p_idx, x, y Planet index and hex coordinates of location
     * @param owner Owning faction
     * @param type Type number (position in UNIT.DAT, 0-91)
     * @param t_lvl Subtype (subordinate position in UNIT.DAT)
     * @param res_relic Resource or relic type (cargo pods and relics only, set
     * to 0 for other units)
     * @param amount Quantity of resources (cargo pods only, set to 0 for other
     * units)
     * @return New unit.
     *
     */
    public Unit createUnitInHex(int p_idx, int x, int y, int owner, int type, int t_lvl, int res_relic, int amount) {    //RSW

        Hex hex = getHexFromPXY(p_idx, x, y);
        List<Unit> stack = hex.getStack();

        if (Util.stackSize(stack) < 20) {    // If stack not full, create new unit
            Unit unit = new Unit(p_idx, x, y, owner, type, t_lvl, res_relic, amount, this);

            units.add(unit);    // Add new unit to the general unit list
            hex.placeUnit(unit);    // Add new unit to the stack
            unmoved_units.add(unit);    // Add new unit to the unmoved units list

//            System.out.println("spotted[] before: " + Arrays.toString(unit.spotted));    //DEBUG
            hex_proc.spotProc(hex, stack);    // Set spotted[] flags for new unit

//            System.out.println("spotted[] after: " + Arrays.toString(unit.spotted));    //DEBUG
            if (unit.type == C.CARGO_UNIT_TYPE) {
                resources.addToPodLists(unit);    // Locations of cargo pods are tracked by class Resources
            }
            if (unit.type_data.eat) {
                resources.addToProdCons(C.CONS, owner, p_idx, C.RES_FOOD, 1);
            }

//            System.out.println("*** createUnitInHex: created " + unit.type_data.name + " at " + p_idx + ", "
//                    + x + ", " + y + ", " + unit.in_space);    // TESTING
            return unit;

        } else {
            return null;
        }
    }

    /**
     * Removes from the game a unit killed in combat (in space or hex). Performs
     * all necessary clean-up.
     *
     * IMPORTANT: Do not call this method while iterating over the general unit
     * list, or any stack or cargo list the unit is on!
     *
     * @param unit Unit to be deleted.
     */
    public void deleteUnitInCombat(Unit unit) {

        // Note: When disembarking units from a list, put them on a temporary list first,
        // to avoid the problem of removing an object from a list while iterating through that list
        List<Unit> temp_list = new LinkedList<>();

        // Disembark any space units on carriers in space
        if (unit.in_space && unit.type == C.SPACE_CARRIER_UNIT_TYPE) {
            for (Unit cargo : unit.cargo_list) {
                if (cargo.type == C.FIGHTER_UNIT_TYPE || cargo.type == C.TORP_BMBR_UNIT_TYPE) {
                    temp_list.add(cargo);
                }
            }
        }
        for (Unit cargo : temp_list) {
            unit.disembark(cargo);
            getUnitStack(unit).add(cargo);
        }

        // Now delete the unit along with any remaining cargo
        deleteUnitAndCargo(unit);
    }

    /**
     * Removes from the game a unit NOT killed in combat. Performs all necessary
     * clean-up.
     *
     * IMPORTANT: Do not call this method while iterating over the general unit
     * list, or any stack or cargo list the unit is on!
     *
     * @param unit Unit to be deleted.
     */
    public void deleteUnitNotInCombat(Unit unit) {

        // Note: When disembarking units from a list, put them on a temporary list first,
        // to avoid the problem of removing an object from a list while iterating through that list
        List<Unit> temp_list = new LinkedList<>();

        // Disembark any cargo which can be disembarked
        for (Unit cargo : unit.cargo_list) {
            if (unit.in_space) {
                if (cargo.move_type == C.MoveType.SPACE
                        || cargo.move_type == C.MoveType.JUMP
                        || cargo.move_type == C.MoveType.LANDER) {
                    temp_list.add(cargo);
                }
            } else {
                boolean[] terrain = getUnitHex(unit).getTerrain();
                int tile_set = getPlanet(unit.p_idx).tile_set_type;
                if (terrain[C.OCEAN] == false || tile_set == C.BARREN_TILE_SET) {
                    temp_list.add(cargo);
                }
            }
        }
        for (Unit cargo : temp_list) {
            unit.disembark(cargo);
            getUnitStack(unit).add(cargo);
        }

        // Now delete the unit along with any remaining cargo
        deleteUnitAndCargo(unit);
    }

    /**
     * Removes a unit from the game along with all its cargo. Not to be called
     * by any method other than deleteUnitInCombat and deleteUnitNotInCombat.
     *
     * @param unit Unit to be deleted.
     */
    private void deleteUnitAndCargo(Unit unit) {

        // Note: When deleting units from a list, put them on a temporary list first,
        // to avoid the problem of removing an object from a list while iterating through that list
        List<Unit> temp_list = new LinkedList<>();

        // First delete all cargo
        for (Unit cargo : unit.cargo_list) {
            temp_list.add(cargo);
        }
        for (Unit cargo : temp_list) {
            deleteEmptyUnit(cargo);
        }

        // Now delete the unit itself
        deleteEmptyUnit(unit);
    }

    /**
     * Removes a unit from the game, when it definitely has no cargo. Not to be
     * called by any method other than deleteUnitAndCargo.
     *
     * @param unit Unit to be deleted.
     */
    private void deleteEmptyUnit(Unit unit) {

        List<Unit> stack = getUnitStack(unit);    // Get unit's stack

        stack.remove(unit);         // Remove unit from its stack
        unmoved_units.remove(unit); // Remove unit from the unmoved unit list
        units.remove(unit);         // Remove unit from the general unit list

        if (unit.carrier != null) {
            unit.carrier.disembark(unit);    // Remove this unit from its carrier's cargo list
        }

        if (unit.type == C.CARGO_UNIT_TYPE) {
            resources.removeFromPodLists(unit);    // Locations of cargo pods are tracked by class Resources
        }

        // Remove planetbound eaters from list of eaters
        if (unit.type_data.eat && !unit.in_space) {
            resources.addToProdCons(C.CONS, unit.owner, unit.p_idx, C.RES_FOOD, -1);
        }
//        System.out.println("*** deleteEmptyUnit: deleted " + unit.type_data.name + " at " + unit.p_idx + ", "
//                + unit.x + ", " + unit.y + ", " + unit.in_space);    // TESTING
        // If there are any units spotted only by this one, we should clear their spotted flags. ???
    }

    /**
     * Relocates a unit to the specified location. All relocation of units must
     * be done through this method, to ensure the updating of secondary data
     * structures that are dependent on the location of units.
     *
     * @param in_space True if the unit is to be in space
     * @param p_idx, x, y Planet index and hex coordinates of new location
     * @param unit Unit to be relocated
     */
    public void relocateUnit(boolean in_space, int p_idx, int x, int y, Unit unit) {    //RSW

        if (unit.type == C.CARGO_UNIT_TYPE) {
            resources.removeFromPodLists(unit);    // Locations of cargo pods are tracked by class Resources
        }
        // units in_space do not eat
        if (unit.type_data.eat && in_space != unit.in_space) {
            if (in_space) {
                resources.addToProdCons(C.CONS, unit.owner, p_idx, C.RES_FOOD, -1);
            } else {
                resources.addToProdCons(C.CONS, unit.owner, p_idx, C.RES_FOOD, 1);
            }
        }
        unit.in_space = in_space;
        unit.p_idx = p_idx;
        unit.x = x;
        unit.y = y;

        if (unit.type == C.CARGO_UNIT_TYPE) {
            resources.addToPodLists(unit);    // Locations of cargo pods are tracked by class Resources
        }

//        System.out.println("*** relocateUnit: " + unit.type_data.name + " relocated to " + p_idx + ", "
//                + x + ", " + y + ", " + in_space);    // TESTING
    }

    /**
     * Changes the ownership of a unit from one faction to another. All changes
     * of ownership must be done through this method, to ensure the updating of
     * secondary data structures that are dependent on the location of units.
     *
     * @param new_owner
     * @param unit
     */
    public void changeOwnerOfUnit(int new_owner, Unit unit) {    //RSW

        if (unit.type == C.CARGO_UNIT_TYPE) {
            resources.removeFromPodLists(unit);    // Ownership of cargo pods is tracked by class Resources
        }

        // update food consumption data
        if (unit.type_data.eat && !unit.in_space) {
            resources.addToProdCons(C.CONS, unit.owner, unit.p_idx, C.RES_FOOD, -1);
            resources.addToProdCons(C.CONS, new_owner, unit.p_idx, C.RES_FOOD, 1);

        }

        unit.owner = new_owner;

        if (unit.type == C.CARGO_UNIT_TYPE) {
            resources.addToPodLists(unit);    // Ownership of cargo pods is tracked by class Resources
        }

//        System.out.println("*** changeOwnerOfUnit: " + unit.type_data.name
//                + " now owned by faction " + new_owner); // TESTING
    }

    /**
     * Returns the hex that a unit's in. IMPORTANT. Be sure unit is in a hex
     * before calling. If necessary, check unit.in_space
     *
     */
    public Hex getUnitHex(Unit unit) {

        Planet planet = planets.get(unit.p_idx);
        Hex hex = planet.planet_grid.getHex(unit.x, unit.y);

        return hex;
    }

    /**
     * Returns the stack that a unit's in (whether in space or in a hex)
     *
     */
    public List<Unit> getUnitStack(Unit unit) {

        List<Unit> stack;

        if (unit.in_space) {
            Planet planet = planets.get(unit.p_idx);
            stack = planet.space_stacks[unit.owner];
        } else {
            Hex hex = getUnitHex(unit);
            stack = hex.getStack();
        }
        return stack;
    }

    /**
     * Returns the hex from a planet index and coordinates
     *
     */
    public Hex getHexFromPXY(int p_idx, int x, int y) {

        Planet planet = planets.get(p_idx);
        Hex hex = planet.planet_grid.getHex(x, y);

        return hex;
    }

    /**
     * Returns whether unit is in a city
     *
     */
    public boolean unitInCity(Unit unit) {

        if (unit.in_space) {
            return false;
        } else {
            Hex hex = getUnitHex(unit);
            if (hex.getStructure() == null) {
                return false;
            } else {
                return true;
            }
        }
    }

}
