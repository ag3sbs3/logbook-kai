package logbook.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import logbook.bean.BattleLog;
import logbook.bean.BattleTypes.AirBaseAttack;
import logbook.bean.BattleTypes.CombinedType;
import logbook.bean.BattleTypes.IAirBaseAttack;
import logbook.bean.BattleTypes.IAirbattle;
import logbook.bean.BattleTypes.IBattle;
import logbook.bean.BattleTypes.ICombinedBattle;
import logbook.bean.BattleTypes.ICombinedEcBattle;
import logbook.bean.BattleTypes.ICombinedEcMidnightBattle;
import logbook.bean.BattleTypes.IHougeki;
import logbook.bean.BattleTypes.IKouku;
import logbook.bean.BattleTypes.IMidnightBattle;
import logbook.bean.BattleTypes.ISortieHougeki;
import logbook.bean.BattleTypes.ISupport;
import logbook.bean.BattleTypes.Kouku;
import logbook.bean.BattleTypes.Raigeki;
import logbook.bean.BattleTypes.Stage3;
import logbook.bean.BattleTypes.SupportAiratack;
import logbook.bean.BattleTypes.SupportHourai;
import logbook.bean.BattleTypes.SupportInfo;
import logbook.bean.Chara;
import logbook.bean.Enemy;
import logbook.bean.Ship;
import lombok.Getter;

/**
 * 戦闘フェイズにおける味方と敵のステータス
 */
@Getter
public class PhaseState {

    /** 連合艦隊 */
    private CombinedType combinedType = CombinedType.未結成;

    /** フェイズ後味方(第1艦隊) */
    private List<Ship> afterFriend = new ArrayList<>();

    /** フェイズ後味方(第2艦隊) */
    private List<Ship> afterFriendCombined = new ArrayList<>();

    /** フェイズ後敵 */
    private List<Enemy> afterEnemy = new ArrayList<>();

    /** フェイズ後敵(第2艦隊) */
    private List<Enemy> afterEnemyCombined = new ArrayList<>();

    /**
     * 戦闘から新規フェイズを作成します
     * @param log 戦闘ログ
     */
    public PhaseState(BattleLog log) {
        this(log.getCombinedType(), log.getBattle(), log.getDeckMap());
    }

    /**
     * 戦闘から新規フェイズを作成します
     * @param combinedType 連合艦隊
     * @param b 戦闘
     * @param deckMap 艦隊スナップショット
     */
    public PhaseState(CombinedType combinedType, IBattle b, Map<Integer, List<Ship>> deckMap) {
        // 連合艦隊
        if (b instanceof ICombinedBattle) {
            this.combinedType = combinedType;
        }
        // 味方
        if (b instanceof ICombinedBattle) {
            for (Ship ship : deckMap.get(1)) {
                this.afterFriend.add(Optional.ofNullable(ship).map(Ship::clone).orElse(null));
            }
            for (Ship ship : deckMap.get(2)) {
                this.afterFriendCombined.add(Optional.ofNullable(ship).map(Ship::clone).orElse(null));
            }
        } else {
            List<Ship> ships;
            if (b instanceof ICombinedEcMidnightBattle) {
                ICombinedEcMidnightBattle ecmb = (ICombinedEcMidnightBattle) b;
                ships = deckMap.get(ecmb.getActiveDeck().get(0));
            } else {
                ships = deckMap.get(b.getDockId());
            }
            for (Ship ship : ships) {
                this.afterFriend.add(Optional.ofNullable(ship).map(Ship::clone).orElse(null));
            }
        }
        // 敵
        for (int i = 1, s = b.getShipKe().size(); i < s; i++) {
            if (b.getShipKe().get(i) != -1) {
                Enemy e = new Enemy();
                e.setShipId(b.getShipKe().get(i));
                e.setLv(b.getShipLv().get(i));
                e.setSlot(b.getESlot().get(i - 1));

                this.afterEnemy.add(e);
            }
        }
        // 敵(第2艦隊)
        if (b instanceof ICombinedEcBattle) {
            ICombinedEcBattle ecb = (ICombinedEcBattle) b;
            for (int i = 1, s = ecb.getShipKeCombined().size(); i < s; i++) {
                if (ecb.getShipKeCombined().get(i) != -1) {
                    Enemy e = new Enemy();
                    e.setShipId(ecb.getShipKeCombined().get(i));
                    e.setLv(ecb.getShipLvCombined().get(i));
                    e.setSlot(ecb.getESlotCombined().get(i - 1));

                    this.afterEnemyCombined.add(e);
                }
            }
        }
        this.setInitialHp(b);
    }

    /**
     * フェイズから新規フェイズを作成します<br>
     * 引数psのフェイズ後が新規フェイズのフェイズ前とフェイズ後にコピーされます
     *
     * @param ps フェイズ
     */
    public PhaseState(PhaseState ps) {
        this.combinedType = ps.combinedType;
        for (Ship ship : ps.afterFriend) {
            this.afterFriend.add(Optional.ofNullable(ship).map(Ship::clone).orElse(null));
        }
        for (Ship ship : ps.afterFriendCombined) {
            this.afterFriendCombined.add(Optional.ofNullable(ship).map(Ship::clone).orElse(null));
        }
        for (Enemy enemy : ps.afterEnemy) {
            this.afterEnemy.add(Optional.ofNullable(enemy).map(Enemy::clone).orElse(null));
        }
        for (Enemy enemy : ps.afterEnemyCombined) {
            this.afterEnemyCombined.add(Optional.ofNullable(enemy).map(Enemy::clone).orElse(null));
        }
    }

    /**
     * 基地航空隊戦フェイズを適用します
     *
     * @param airBaseAttack 基地航空隊戦フェイズ
     */
    public void applyAirBaseAttack(IAirBaseAttack airBaseAttack) {
        List<AirBaseAttack> attacks = airBaseAttack.getAirBaseAttack();
        if (attacks != null) {
            for (AirBaseAttack attack : attacks) {
                Stage3 stage3 = attack.getStage3();
                if (stage3 != null) {
                    List<Double> edam = stage3.getEdam();
                    if (edam != null) {
                        this.applyEnemyDamage(edam, this.afterEnemy);
                    }
                }
                Stage3 stage3Combined = attack.getStage3Combined();
                if (stage3Combined != null) {
                    List<Double> edam = stage3Combined.getEdam();
                    if (edam != null) {
                        this.applyEnemyDamage(edam, this.afterEnemyCombined);
                    }
                }
            }
        }
    }

    /**
     * 航空戦フェイズを適用します
     * @param battle 航空戦フェイズ
     */
    public void applyKouku(IKouku battle) {
        this.applyKouku(battle.getKouku());
    }

    /**
     * 支援フェイズを適用します
     * @param battle 支援フェイズ
     */
    public void applySupport(ISupport battle) {
        SupportInfo support = battle.getSupportInfo();
        if (support != null) {
            SupportAiratack air = support.getSupportAiratack();
            if (air != null) {
                Stage3 stage3 = air.getStage3();
                if (stage3 != null) {
                    this.applyEnemyDamage(stage3.getEdam(), this.afterEnemy);
                }
            }
            SupportHourai hou = support.getSupportHourai();
            if (hou != null) {
                this.applyEnemyDamage(hou.getDamage(), this.afterEnemy);
            }
        }
    }

    /**
     * 昼戦砲雷撃戦フェイズを適用します
     * @param battle 昼戦砲雷撃戦フェイズ
     */
    public void applySortieHougeki(ISortieHougeki battle) {
        // 先制対潜攻撃
        this.applyHougeki(battle.getOpeningTaisen(), this.afterFriend, this.afterEnemy);
        // 開幕雷撃
        this.applyRaigeki(battle.getOpeningAtack());
        if (battle instanceof ICombinedEcBattle) {
            // 連合艦隊(敵)
            // 1巡目
            this.applyHougeki(battle.getHougeki1());
            // 雷撃
            this.applyRaigeki(battle.getRaigeki());
            // 1巡目
            this.applyHougeki(battle.getHougeki2());
            // 2巡目
            this.applyHougeki(battle.getHougeki3());
        } else if (this.combinedType == CombinedType.未結成) {
            // 連合艦隊以外
            // 1巡目
            this.applyHougeki(battle.getHougeki1(), this.afterFriend, this.afterEnemy);
            // 2巡目
            this.applyHougeki(battle.getHougeki2(), this.afterFriend, this.afterEnemy);
            // 雷撃
            this.applyRaigeki(battle.getRaigeki());
        } else if (this.combinedType == CombinedType.機動部隊 || this.combinedType == CombinedType.輸送部隊) {
            // 空母機動部隊、輸送護衛部隊
            // 第2艦隊1巡目
            this.applyHougeki(battle.getHougeki1(), this.afterFriendCombined, this.afterEnemy);
            // 第2艦隊雷撃
            this.applyRaigeki(battle.getRaigeki());
            // 第1艦隊1巡目
            this.applyHougeki(battle.getHougeki2(), this.afterFriend, this.afterEnemy);
            // 第1艦隊2巡目
            this.applyHougeki(battle.getHougeki3(), this.afterFriend, this.afterEnemy);
        } else if (this.combinedType == CombinedType.水上部隊) {
            // 水上打撃部隊
            // 第1艦隊1巡目
            this.applyHougeki(battle.getHougeki1(), this.afterFriend, this.afterEnemy);
            // 第1艦隊2巡目
            this.applyHougeki(battle.getHougeki2(), this.afterFriend, this.afterEnemy);
            // 第2艦隊1巡目
            this.applyHougeki(battle.getHougeki3(), this.afterFriendCombined, this.afterEnemy);
            // 第2艦隊雷撃
            this.applyRaigeki(battle.getRaigeki());
        }
    }

    /**
     * 航空戦を適用します
     * @param battle 航空戦
     */
    public void applyAirbattle(IAirbattle battle) {
        this.applyKouku(battle.getKouku2());
    }

    /**
     * 夜戦を適用します
     * @param battle 夜戦
     */
    public void applyMidnightBattle(IMidnightBattle battle) {
        if (battle instanceof ICombinedEcMidnightBattle) {
            ICombinedEcMidnightBattle ecBattle = (ICombinedEcMidnightBattle) battle;

            List<Ship> friend;
            if (ecBattle.getActiveDeck().get(0) == 1) {
                friend = this.afterFriend;
            } else {
                friend = this.afterFriendCombined;
            }
            List<Enemy> enemy;
            if (ecBattle.getActiveDeck().get(1) == 1) {
                enemy = this.afterEnemy;
            } else {
                enemy = this.afterEnemyCombined;
            }
            this.applyHougeki(battle.getHougeki(), friend, enemy);
        } else if (this.combinedType == CombinedType.未結成) {
            // 連合艦隊以外
            this.applyHougeki(battle.getHougeki(), this.afterFriend, this.afterEnemy);
        } else {
            // 連合艦隊
            this.applyHougeki(battle.getHougeki(), this.afterFriendCombined, this.afterEnemy);
        }
    }

    /**
     * 全てのフェイズを適用します
     * @param battle 戦闘
     */
    public void apply(IBattle battle) {
        if (battle == null) {
            return;
        }

        // 基地航空隊戦フェイズ
        if (battle instanceof IAirBaseAttack) {
            this.applyAirBaseAttack((IAirBaseAttack) battle);
        }
        // 航空戦フェイズ
        if (battle instanceof IKouku) {
            if (((IKouku) battle).getKouku() != null) {
                this.applyKouku((IKouku) battle);
            }
        }
        // 支援フェイズ
        if (battle instanceof ISupport) {
            if (((ISupport) battle).getSupportInfo() != null) {
                this.applySupport((ISupport) battle);
            }
        }
        // 砲雷撃戦フェイズ
        if (battle instanceof ISortieHougeki) {
            this.applySortieHougeki((ISortieHougeki) battle);
        }
        // 航空戦
        if (battle instanceof IAirbattle) {
            this.applyAirbattle((IAirbattle) battle);
        }
        // 夜戦
        if (battle instanceof IMidnightBattle) {
            this.applyMidnightBattle((IMidnightBattle) battle);
        }
    }

    /**
     * 航空戦フェイズを適用します
     * @param kouku 航空戦フェイズ
     */
    private void applyKouku(Kouku kouku) {
        if (kouku == null) {
            return;
        }

        Stage3 stage3 = kouku.getStage3();
        if (stage3 != null) {
            this.applyFriendDamage(stage3.getFdam(), this.afterFriend);
            this.applyEnemyDamage(stage3.getEdam(), this.afterEnemy);
        }
        Stage3 stage3Combined = kouku.getStage3Combined();
        if (stage3Combined != null) {
            this.applyFriendDamage(stage3Combined.getFdam(), this.afterFriendCombined);
            if (stage3Combined.getEdam() != null) {
                this.applyEnemyDamage(stage3Combined.getEdam(), this.afterEnemyCombined);
            }
        }
    }

    /**
     * 雷撃戦フェイズを適用します
     * @param raigeki 雷撃戦フェイズ
     */
    private void applyRaigeki(Raigeki raigeki) {
        if (raigeki == null) {
            return;
        }

        if (this.combinedType != CombinedType.未結成) {
            // 連合艦隊の場合、第2艦隊にダメージを適用
            this.applyFriendDamage(raigeki.getFdam(), this.afterFriendCombined);
        } else {
            // 連合艦隊以外の場合
            this.applyFriendDamage(raigeki.getFdam(), this.afterFriend);
        }
        // 敵
        this.applyEnemyDamage(raigeki.getEdam());
    }

    /**
     * 砲撃戦フェイズを適用します
     * @param hougeki 砲撃戦フェイズ
     * @param friend 交戦する味方
     * @param enemy 交戦する敵
     */
    private void applyHougeki(IHougeki hougeki, List<Ship> friend, List<Enemy> enemy) {
        if (hougeki == null) {
            return;
        }

        if (hougeki.getAtEflag() == null) {
            for (int i = 1, s = hougeki.getDamage().size(); i < s; i++) {
                // 防御側インデックス(1-6,7-12)
                int df = hougeki.getDfList().get(i).get(0);
                int damage = hougeki.getDamage().get(i)
                        .stream()
                        .mapToInt(Double::intValue)
                        .filter(d -> d > 0)
                        .sum();
                if (df <= 6) {
                    // 6以下は味方
                    Ship ship = friend.get(df - 1);
                    if (ship != null) {
                        ship.setNowhp(ship.getNowhp() - damage);
                    }
                } else {
                    // 7以上は敵
                    Enemy ship = enemy.get(df - 1 - 6);
                    ship.setNowhp(ship.getNowhp() - damage);
                }
            }
        } else {
            this.applyHougeki(hougeki);
        }
    }

    /**
     * 砲撃戦フェイズを適用します(新API用)
     * @param hougeki 砲撃戦フェイズ
     */
    private void applyHougeki(IHougeki hougeki) {
        if (hougeki == null) {
            return;
        }

        for (int i = 1, s = hougeki.getDamage().size(); i < s; i++) {
            // 防御側インデックス(1-6,7-12)
            int df = hougeki.getDfList().get(i).get(0);
            int damage = hougeki.getDamage().get(i)
                    .stream()
                    .mapToInt(Double::intValue)
                    .filter(d -> d > 0)
                    .sum();
            // 攻撃側が味方の場合true
            boolean atkfriend = hougeki.getAtEflag().get(i) == 0;
            Chara target;
            if (df <= 6) {
                // 6以下は第1艦隊
                List<? extends Chara> tl;
                if (atkfriend) {
                    tl = this.afterEnemy;
                } else {
                    tl = this.afterFriend;
                }
                target = tl.get(df - 1);
            } else {
                // 7以上は第2艦隊
                List<? extends Chara> tl;
                if (atkfriend) {
                    tl = this.afterEnemyCombined;
                } else {
                    tl = this.afterFriendCombined;
                }
                target = tl.get(df - 1 - 6);
            }
            if (target != null) {
                target.setNowhp(target.getNowhp() - damage);
            }
        }
    }

    /**
     * ダメージを適用します
     * @param damage ダメージ(one-based)
     * @param friend 味方
     */
    private void applyFriendDamage(List<Double> damages, List<Ship> friend) {
        for (int i = 1, s = damages.size(); i < s; i++) {
            int damage = damages.get(i).intValue();
            if (damage != 0) {
                Ship ship = friend.get(i - 1);
                if (ship != null) {
                    ship.setNowhp(ship.getNowhp() - damage);
                }
            }
        }
    }

    /**
     * ダメージを適用します(敵第1艦隊(+第2艦隊)固定の場合に使用)
     * @param damage ダメージ(one-based)
     */
    private void applyEnemyDamage(List<Double> damages) {
        for (int i = 1, s = damages.size(); i < s; i++) {
            int damage = damages.get(i).intValue();
            if (damage != 0) {
                Enemy enemy;
                if (i <= 6) {
                    enemy = this.afterEnemy.get(i - 1);
                } else {
                    enemy = this.afterEnemyCombined.get(i - (6 + 1));
                }
                if (enemy != null) {
                    enemy.setNowhp(enemy.getNowhp() - damage);
                }
            }
        }
    }

    /**
     * ダメージを適用します(敵艦隊指定の場合に使用)
     * @param damage ダメージ(one-based)
     * @param enemies 敵
     */
    private void applyEnemyDamage(List<Double> damages, List<Enemy> enemies) {
        for (int i = 1, s = damages.size(); i < s; i++) {
            int damage = damages.get(i).intValue();
            if (damage != 0) {
                Enemy enemy = enemies.get(i - 1);
                if (enemy != null) {
                    enemy.setNowhp(enemy.getNowhp() - damage);
                }
            }
        }
    }

    /**
     * HPをセットします
     * @param b 戦闘
     */
    private void setInitialHp(IBattle b) {
        for (int i = 1, s = b.getMaxhps().size(); i < s; i++) {
            if (b.getMaxhps().get(i) == -1) {
                continue;
            }
            if (i <= 6) {
                int idx = i - 1;
                if (this.afterFriend.get(idx) != null) {
                    this.afterFriend.get(idx).setMaxhp(b.getMaxhps().get(i));
                    this.afterFriend.get(idx).setNowhp(b.getNowhps().get(i));
                }
            } else {
                int idx = i - 1 - 6;
                if (this.afterEnemy.get(idx) != null) {
                    this.afterEnemy.get(idx).setMaxhp(b.getMaxhps().get(i));
                    this.afterEnemy.get(idx).setNowhp(b.getNowhps().get(i));
                }
            }
        }
        if (b instanceof ICombinedBattle) {
            ICombinedBattle cb = (ICombinedBattle) b;
            for (int i = 1, s = 6 + 1; i < s; i++) {
                int idx = i - 1;
                if (cb.getMaxhpsCombined().get(i) == -1) {
                    continue;
                }
                if (this.afterFriendCombined.get(idx) != null) {
                    this.afterFriendCombined.get(idx).setMaxhp(cb.getMaxhpsCombined().get(i));
                    this.afterFriendCombined.get(idx).setNowhp(cb.getNowhpsCombined().get(i));
                }
            }
        }
        if (b instanceof ICombinedEcBattle) {
            ICombinedEcBattle ecb = (ICombinedEcBattle) b;
            for (int i = 6 + 1, s = 12 + 1; i < s; i++) {
                int idx = i - (6 + 1);
                if (ecb.getMaxhpsCombined().get(i) == -1) {
                    continue;
                }
                if (this.afterEnemyCombined.get(idx) != null) {
                    this.afterEnemyCombined.get(idx).setMaxhp(ecb.getMaxhpsCombined().get(i));
                    this.afterEnemyCombined.get(idx).setNowhp(ecb.getNowhpsCombined().get(i));
                }
            }
        }
    }
}