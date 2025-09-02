package catgirlroutes.utils

enum class Island(val displayName: String) {
    SinglePlayer("Singleplayer"),
    PrivateIsland("Private Island"),
    Garden("The Garden"),
    SpiderDen("Spider's Den"),
    CrimsonIsle("Crimson Isle"),
    TheEnd("The End"),
    GoldMine("Gold Mine"),
    DeepCaverns("Deep Caverns"),
    DwarvenMines("Dwarven Mines"),
    CrystalHollows("Crystal Hollows"),
    FarmingIsland("The Farming Islands"),
    ThePark("The Park"),
    Dungeon("Catacombs"),
    DungeonHub("Dungeon Hub"),
    Hub("Hub"),
    DarkAuction("Dark Auction"),
    JerryWorkshop("Jerry's Workshop"),
    Kuudra("Kuudra"),
    Rift("Rift"),
    Mineshaft("Glacite Mineshafts"),
    BackwaterBayou("Backwater Bayou"),
    Galatea("Galatea"), // stupid but idc
    Unknown("(Unknown)");

    fun isArea(area: Island): Boolean = this == area

    fun isArea(vararg areas: Island): Boolean = this in areas
}