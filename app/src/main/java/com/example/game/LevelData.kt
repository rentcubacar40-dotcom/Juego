package com.example.game

data class LevelMission(
    val id: Int,
    val title: String,
    val codename: String,
    val location: String,
    val briefing: String,
    val primaryObjective: String,
    val secondaryObjective: String,
    val targetKills: Int,
    val mapWidth: Int,
    val mapHeight: Int,
    val map: Array<IntArray>,
    val playerStartX: Float,
    val playerStartY: Float,
    val playerStartAngle: Float,
    val enemies: List<Enemy>,
    val items: List<WorldItem>,
    val creditReward: Int,
    val themeColor: Long,
    val isNightOps: Boolean = false,
    val isEndless: Boolean = false
)

object LevelData {
    fun getLevel(id: Int): LevelMission {
        return when (id) {
            1 -> createLevel1()
            2 -> createLevel2()
            3 -> createLevel3()
            else -> createLevel4()
        }
    }

    private fun createLevel1(): LevelMission {
        // 16x16 Warehouse Breach
        val map = arrayOf(
            intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
            intArrayOf(1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,3,3,0,0,1,0,3,3,3,0,0,3,0,1),
            intArrayOf(1,0,3,3,0,0,0,0,0,0,0,0,0,3,0,1),
            intArrayOf(1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,1,1,4,1,1,1,1,1,0,1,1,1,1,1,1),
            intArrayOf(1,0,0,0,0,0,0,0,1,0,1,0,0,0,0,1),
            intArrayOf(1,0,3,0,0,3,0,0,1,0,1,0,3,3,0,1),
            intArrayOf(1,0,3,0,0,3,0,0,0,0,0,0,3,3,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,1,0,1,0,0,0,0,1),
            intArrayOf(1,1,1,0,1,1,1,1,1,0,1,1,4,1,1,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,3,3,3,0,0,2,2,0,0,3,3,3,0,1),
            intArrayOf(1,0,0,0,0,0,0,2,2,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
        )

        val enemies = listOf(
            Enemy(1, EnemyType.RECON, 3.5f, 2.5f, 0f, patrolWaypoints = listOf(3.5f to 2.5f, 5.5f to 2.5f)),
            Enemy(2, EnemyType.RECON, 11.5f, 3.5f, 3.14f, patrolWaypoints = listOf(11.5f to 3.5f, 9.5f to 3.5f)),
            Enemy(3, EnemyType.MERCENARY, 3.5f, 7.5f, 1.57f, patrolWaypoints = listOf(3.5f to 7.5f, 6.5f to 7.5f)),
            Enemy(4, EnemyType.MERCENARY, 13.5f, 8.5f, 3.14f, patrolWaypoints = listOf(13.5f to 8.5f, 11.5f to 8.5f)),
            Enemy(5, EnemyType.RECON, 7.5f, 13.5f, 0f, patrolWaypoints = listOf(7.5f to 13.5f, 12.5f to 13.5f)),
            Enemy(6, EnemyType.MERCENARY, 13.5f, 13.5f, 3.14f, patrolWaypoints = listOf(13.5f to 13.5f, 13.5f to 11.5f))
        )

        val items = listOf(
            WorldItem(1, ItemType.EXPLOSIVE_BARREL, 4.5f, 3.5f),
            WorldItem(2, ItemType.EXPLOSIVE_BARREL, 12.5f, 7.5f),
            WorldItem(3, ItemType.MEDKIT, 1.5f, 4.5f),
            WorldItem(4, ItemType.AMMO_CRATE, 14.5f, 4.5f),
            WorldItem(5, ItemType.ARMOR_VEST, 1.5f, 13.5f),
            WorldItem(6, ItemType.MISSION_INTEL, 7.5f, 14.2f)
        )

        return LevelMission(
            id = 1,
            title = "Operación Almacén",
            codename = "IRON BREACH",
            location = "Sector Portuario 4",
            briefing = "Fuerzas hostiles han asegurado un alijo de armas militares. Infíltrate, neutraliza las patrullas enemigas y recupera la computadora con información clasificada.",
            primaryObjective = "Elimina a los 6 hostiles y recupera el maletín de inteligencia",
            secondaryObjective = "Logra al menos 3 bajas por tiro a la cabeza",
            targetKills = 6,
            mapWidth = 16,
            mapHeight = 16,
            map = map,
            playerStartX = 1.5f,
            playerStartY = 1.5f,
            playerStartAngle = 0.5f,
            enemies = enemies,
            items = items,
            creditReward = 1500,
            themeColor = 0xFFF59E0B
        )
    }

    private fun createLevel2(): LevelMission {
        // 16x16 Desert Outpost with explosive silos and Boss
        val map = arrayOf(
            intArrayOf(5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5),
            intArrayOf(5,0,0,0,0,5,0,0,0,0,5,0,0,0,0,5),
            intArrayOf(5,0,3,0,0,0,0,3,3,0,0,0,0,3,0,5),
            intArrayOf(5,0,3,0,0,5,0,3,3,0,5,0,0,3,0,5),
            intArrayOf(5,0,0,0,0,5,0,0,0,0,5,0,0,0,0,5),
            intArrayOf(5,5,0,5,5,5,5,0,0,5,5,5,5,0,5,5),
            intArrayOf(5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5),
            intArrayOf(5,0,3,3,0,0,5,5,5,5,0,0,3,3,0,5),
            intArrayOf(5,0,3,3,0,0,5,2,2,5,0,0,3,3,0,5),
            intArrayOf(5,0,0,0,0,0,5,0,0,5,0,0,0,0,0,5),
            intArrayOf(5,5,0,5,5,5,5,0,0,5,5,5,5,0,5,5),
            intArrayOf(5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5),
            intArrayOf(5,0,3,0,0,5,0,0,0,0,5,0,0,3,0,5),
            intArrayOf(5,0,3,0,0,5,0,3,3,0,5,0,0,3,0,5),
            intArrayOf(5,0,0,0,0,0,0,3,3,0,0,0,0,0,0,5),
            intArrayOf(5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5)
        )

        val enemies = listOf(
            Enemy(1, EnemyType.SNIPER, 14.5f, 2.5f, 3.14f),
            Enemy(2, EnemyType.MERCENARY, 6.5f, 3.5f, 1.57f),
            Enemy(3, EnemyType.RECON, 2.5f, 6.5f, 0f),
            Enemy(4, EnemyType.MERCENARY, 13.5f, 6.5f, 3.14f),
            Enemy(5, EnemyType.SNIPER, 1.5f, 13.5f, 0.7f),
            Enemy(6, EnemyType.MERCENARY, 11.5f, 12.5f, 3.14f),
            Enemy(7, EnemyType.COMMANDER, 8.5f, 8.5f, 4.71f) // Boss
        )

        val items = listOf(
            WorldItem(1, ItemType.EXPLOSIVE_BARREL, 7.5f, 3.5f),
            WorldItem(2, ItemType.EXPLOSIVE_BARREL, 8.5f, 3.5f),
            WorldItem(3, ItemType.EXPLOSIVE_BARREL, 3.5f, 8.5f),
            WorldItem(4, ItemType.EXPLOSIVE_BARREL, 12.5f, 8.5f),
            WorldItem(5, ItemType.MEDKIT, 1.5f, 2.5f),
            WorldItem(6, ItemType.AMMO_CRATE, 14.5f, 13.5f),
            WorldItem(7, ItemType.ARMOR_VEST, 7.5f, 8.5f)
        )

        return LevelMission(
            id = 2,
            title = "Asalto al Búnker",
            codename = "DESERT VIPER",
            location = "Sector Desértico Bravo",
            briefing = "El Comandante Boris ha fortificado una base en el desierto con depósitos de combustible volátiles. Elimina al comandante y destruye las reservas enemigas.",
            primaryObjective = "Elimina a todos los 7 enemigos incluyendo al Comandante Boris",
            secondaryObjective = "Detona barriles explosivos para eliminar enemigos",
            targetKills = 7,
            mapWidth = 16,
            mapHeight = 16,
            map = map,
            playerStartX = 1.5f,
            playerStartY = 1.5f,
            playerStartAngle = 0.8f,
            enemies = enemies,
            items = items,
            creditReward = 2800,
            themeColor = 0xFFEF4444
        )
    }

    private fun createLevel3(): LevelMission {
        // 16x16 Arctic Cyber Ops (Night Ops with NVG goggles!)
        val map = arrayOf(
            intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
            intArrayOf(1,0,0,0,1,0,0,0,0,0,0,1,0,0,0,1),
            intArrayOf(1,0,2,0,1,0,2,2,2,2,0,1,0,2,0,1),
            intArrayOf(1,0,2,0,0,0,0,0,0,0,0,0,0,2,0,1),
            intArrayOf(1,0,0,0,1,0,2,2,2,2,0,1,0,0,0,1),
            intArrayOf(1,1,4,1,1,0,0,0,0,0,0,1,1,4,1,1),
            intArrayOf(1,0,0,0,0,0,1,1,1,1,0,0,0,0,0,1),
            intArrayOf(1,0,2,2,0,0,1,2,2,1,0,0,2,2,0,1),
            intArrayOf(1,0,2,2,0,0,4,0,0,4,0,0,2,2,0,1),
            intArrayOf(1,0,0,0,0,0,1,1,1,1,0,0,0,0,0,1),
            intArrayOf(1,1,4,1,1,0,0,0,0,0,0,1,1,4,1,1),
            intArrayOf(1,0,0,0,1,0,2,2,2,2,0,1,0,0,0,1),
            intArrayOf(1,0,2,0,0,0,0,0,0,0,0,0,0,2,0,1),
            intArrayOf(1,0,2,0,1,0,2,2,2,2,0,1,0,2,0,1),
            intArrayOf(1,0,0,0,1,0,0,0,0,0,0,1,0,0,0,1),
            intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
        )

        val enemies = listOf(
            Enemy(1, EnemyType.SNIPER, 8.5f, 1.5f, 1.57f),
            Enemy(2, EnemyType.RECON, 3.5f, 3.5f, 0f),
            Enemy(3, EnemyType.MERCENARY, 12.5f, 3.5f, 3.14f),
            Enemy(4, EnemyType.MERCENARY, 3.5f, 7.5f, 1.57f),
            Enemy(5, EnemyType.SNIPER, 12.5f, 7.5f, 3.14f),
            Enemy(6, EnemyType.MERCENARY, 3.5f, 11.5f, 0f),
            Enemy(7, EnemyType.RECON, 12.5f, 11.5f, 3.14f),
            Enemy(8, EnemyType.COMMANDER, 8.5f, 8.5f, 4.71f)
        )

        val items = listOf(
            WorldItem(1, ItemType.EXPLOSIVE_BARREL, 5.5f, 4.5f),
            WorldItem(2, ItemType.EXPLOSIVE_BARREL, 10.5f, 4.5f),
            WorldItem(3, ItemType.EXPLOSIVE_BARREL, 5.5f, 10.5f),
            WorldItem(4, ItemType.EXPLOSIVE_BARREL, 10.5f, 10.5f),
            WorldItem(5, ItemType.MEDKIT, 1.5f, 1.5f),
            WorldItem(6, ItemType.ARMOR_VEST, 14.5f, 1.5f),
            WorldItem(7, ItemType.AMMO_CRATE, 1.5f, 14.5f),
            WorldItem(8, ItemType.MISSION_INTEL, 8.5f, 8.5f)
        )

        return LevelMission(
            id = 3,
            title = "Laboratorio Ártico",
            codename = "FROSTBYTE ZERO",
            location = "Instalación Subterránea Boreal",
            briefing = "Incursión nocturna en un complejo de ciber-guerra blindado. Activa tus gafas de visión nocturna (NVG), neutraliza la seguridad de élite y asegura la unidad cuántica.",
            primaryObjective = "Hackea el servidor central y elimina a los 8 operadores hostiles",
            secondaryObjective = "Completa la misión usando el visor nocturno NVG",
            targetKills = 8,
            mapWidth = 16,
            mapHeight = 16,
            map = map,
            playerStartX = 1.5f,
            playerStartY = 7.5f,
            playerStartAngle = 0f,
            enemies = enemies,
            items = items,
            creditReward = 4500,
            themeColor = 0xFF06B6D4,
            isNightOps = true
        )
    }

    private fun createLevel4(): LevelMission {
        // Endless Survival Arena
        val map = arrayOf(
            intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
            intArrayOf(1,0,0,0,0,0,0,1,1,0,0,0,0,0,0,1),
            intArrayOf(1,0,3,3,0,0,0,0,0,0,0,0,3,3,0,1),
            intArrayOf(1,0,3,3,0,1,0,0,0,0,1,0,3,3,0,1),
            intArrayOf(1,0,0,0,0,1,0,3,3,0,1,0,0,0,0,1),
            intArrayOf(1,0,0,1,1,1,0,3,3,0,1,1,1,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,1,0,0,3,0,0,2,2,0,0,3,0,0,1,1),
            intArrayOf(1,1,0,0,3,0,0,2,2,0,0,3,0,0,1,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,1,1,1,0,3,3,0,1,1,1,0,0,1),
            intArrayOf(1,0,0,0,0,1,0,3,3,0,1,0,0,0,0,1),
            intArrayOf(1,0,3,3,0,1,0,0,0,0,1,0,3,3,0,1),
            intArrayOf(1,0,3,3,0,0,0,0,0,0,0,0,3,3,0,1),
            intArrayOf(1,0,0,0,0,0,0,1,1,0,0,0,0,0,0,1),
            intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
        )

        val enemies = listOf(
            Enemy(1, EnemyType.RECON, 3.5f, 2.5f, 0f),
            Enemy(2, EnemyType.MERCENARY, 12.5f, 2.5f, 3.14f),
            Enemy(3, EnemyType.RECON, 3.5f, 13.5f, 0f),
            Enemy(4, EnemyType.MERCENARY, 12.5f, 13.5f, 3.14f),
            Enemy(5, EnemyType.SNIPER, 8.5f, 1.5f, 1.57f)
        )

        val items = listOf(
            WorldItem(1, ItemType.EXPLOSIVE_BARREL, 4.5f, 4.5f),
            WorldItem(2, ItemType.EXPLOSIVE_BARREL, 11.5f, 4.5f),
            WorldItem(3, ItemType.EXPLOSIVE_BARREL, 4.5f, 11.5f),
            WorldItem(4, ItemType.EXPLOSIVE_BARREL, 11.5f, 11.5f),
            WorldItem(5, ItemType.MEDKIT, 8.5f, 5.5f),
            WorldItem(6, ItemType.AMMO_CRATE, 8.5f, 10.5f),
            WorldItem(7, ItemType.ARMOR_VEST, 8.5f, 7.5f)
        )

        return LevelMission(
            id = 4,
            title = "Supervivencia Black Site",
            codename = "APEX GAUNTLET",
            location = "Instalación Clasificada de Pruebas",
            briefing = "Simulación táctica en arena confinada con oleadas continuas de fuerzas especiales. Pon a prueba tus reflejos y armamento hasta el límite.",
            primaryObjective = "Sobrevive a tantas oleadas como puedas y acumula récord de bajas",
            secondaryObjective = "Supera la oleada 5",
            targetKills = 25,
            mapWidth = 16,
            mapHeight = 16,
            map = map,
            playerStartX = 8.5f,
            playerStartY = 8.5f,
            playerStartAngle = 0f,
            enemies = enemies,
            items = items,
            creditReward = 6000,
            themeColor = 0xFF10B981,
            isEndless = true
        )
    }
}
