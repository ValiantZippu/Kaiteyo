package ua.syt0r.kanji.core.world

import kotlin.random.Random

// ============================================================
// WORLD — BUILDINGS, VEHICLES & TRAINS
// ------------------------------------------------------------
// Buildings are placed along roads and landmarks. Vehicles
// travel along road networks; trains travel along rail lines
// with station stops. All positions are in world space.
// ============================================================

/**
 * Building placement result — the buildings generated for a chunk.
 */
data class BuildingPlacement(
    val buildings: List<ChunkBuilding> = emptyList(),
    val roads: List<ChunkRoad> = emptyList()
)

/**
 * Building generator — produces plausible Japanese town buildings
 * along roads, with set-aside plots for landmarks (temple, shrine,
 * station). Positions are chunk-local.
 */
class BuildingGenerator(
    private val landmarkSites: Map<String, WorldPosition> = emptyMap(),
    private val seed: Long = 7
) {

    /**
     * Generate buildings for a chunk. [roadPoints] are world-space
     * road centerlines crossing the chunk.
     */
    fun generateForChunk(
        coord: ChunkCoord,
        roadPoints: List<List<Pair<Double, Double>>> = emptyList(),
        buildingCount: Int = 8
    ): BuildingPlacement {
        val origin = coord.worldOrigin
        val rng = Random(seed * 31 + coord.cx * 17 + coord.cz * 13)
        val buildings = mutableListOf<ChunkBuilding>()

        // Local roads: simple grid aligned with the chunk.
        val roads = buildList {
            // Horizontal street
            add(ChunkRoad(
                id = "road-h-${coord.cx}-${coord.cz}",
                points = listOf(0.0 to CHUNK_SIZE / 2, CHUNK_SIZE to CHUNK_SIZE / 2),
                widthMeters = 8.0,
                type = RoadType.Street
            ))
            // Vertical street
            add(ChunkRoad(
                id = "road-v-${coord.cx}-${coord.cz}",
                points = listOf(CHUNK_SIZE / 2 to 0.0, CHUNK_SIZE / 2 to CHUNK_SIZE),
                widthMeters = 8.0,
                type = RoadType.Street
            ))
        }

        // Houses in the quadrants between the roads.
        val quadrantCenters = listOf(
            0.25 to 0.25,
            0.75 to 0.25,
            0.25 to 0.75,
            0.75 to 0.75
        )
        val perQuadrant = (buildingCount / 4).coerceAtLeast(1)
        var index = 0
        for ((qx, qz) in quadrantCenters) {
            repeat(perQuadrant) {
                val localX = qx * CHUNK_SIZE + rng.nextDouble(-20.0, 20.0)
                val localZ = qz * CHUNK_SIZE + rng.nextDouble(-20.0, 20.0)
                buildings.add(ChunkBuilding(
                    id = "building-${coord.cx}-${coord.cz}-$index",
                    name = houseName(index, rng),
                    localX = localX,
                    localZ = localZ,
                    widthMeters = rng.nextDouble(6.0, 10.0),
                    depthMeters = rng.nextDouble(6.0, 10.0),
                    heightMeters = rng.nextDouble(5.0, 9.0),
                    type = BuildingType.House,
                    style = rng.nextInt(0, 4)
                ))
                index++
            }
        }

        // Landmark sites (world-space) that fall inside this chunk.
        for ((id, worldPos) in landmarkSites) {
            val localX = worldPos.x - origin.x
            val localZ = worldPos.z - origin.z
            if (localX in 0.0..CHUNK_SIZE && localZ in 0.0..CHUNK_SIZE) {
                buildings.add(ChunkBuilding(
                    id = "landmark-$id",
                    name = id,
                    localX = localX,
                    localZ = localZ,
                    widthMeters = 30.0,
                    depthMeters = 30.0,
                    heightMeters = 20.0,
                    type = if (id.contains("temple") || id.contains("der")) BuildingType.Temple
                    else if (id.contains("shrine") || id.contains("gu")) BuildingType.Shrine
                    else if (id.contains("station") || id.contains("eki")) BuildingType.Station
                    else BuildingType.Other,
                    hasInterior = id.contains("station")
                ))
            }
        }

        return BuildingPlacement(buildings = buildings, roads = roads)
    }

    private fun houseName(index: Int, rng: Random): String {
        val families = listOf("佐藤", "鈴木", "高橋", "田中", "伊藤", "渡辺", "山本", "中村")
        return "${families[rng.nextInt(families.size)]}邸"
    }
}

// ============================================================
// VEHICLES
// ============================================================

/**
 * Vehicle types.
 */
enum class VehicleType(val label: String) {
    Car("Car"),
    Bus("Bus"),
    Truck("Truck"),
    Taxi("Taxi"),
    Bicycle("Bicycle"),
    Motorcycle("Motorcycle"),
    Train("Train"),
    Police("Police car"),
    Ambulance("Ambulance")
}

/**
 * A vehicle moving through the world.
 */
data class Vehicle(
    val id: String,
    val type: VehicleType,
    val position: WorldPosition,
    val speed: Double,
    val headingDegrees: Float,
    val route: List<WorldPosition> = emptyList(),
    val routeIndex: Int = 0,
    val isMoving: Boolean = true,
    val occupiedBy: String? = null
)

/**
 * Vehicle system — updates vehicles along their routes.
 */
class VehicleSystem(
    private val vehicleLimit: Int = 20
) : WorldSystem {

    private val vehicles = mutableListOf<Vehicle>()

    val activeVehicles: List<Vehicle> get() = vehicles.toList()

    fun addVehicle(vehicle: Vehicle) {
        if (vehicles.size < vehicleLimit) vehicles.add(vehicle)
    }

    fun removeVehicle(id: String) {
        vehicles.removeAll { it.id == id }
    }

    fun clear() = vehicles.clear()

    override suspend fun onStart(runtime: WorldRuntime) {}

    override suspend fun onStop(runtime: WorldRuntime) {
        vehicles.clear()
    }

    override suspend fun onUpdate(deltaSeconds: Double, runtime: WorldRuntime) {
        for (i in vehicles.indices) {
            val vehicle = vehicles[i]
            if (!vehicle.isMoving || vehicle.route.isEmpty()) continue

            val target = vehicle.route[vehicle.routeIndex]
            val delta = target - vehicle.position
            val distance = delta.horizontalDistanceTo(WorldPosition.Zero)
            val step = vehicle.speed * deltaSeconds

            if (distance <= step) {
                // Reached waypoint — advance
                val nextIndex = (vehicle.routeIndex + 1) % vehicle.route.size
                vehicles[i] = vehicle.copy(
                    position = target,
                    routeIndex = nextIndex
                )
            } else {
                val factor = step / distance
                val newPos = vehicle.position + delta * factor
                vehicles[i] = vehicle.copy(position = newPos)
            }
        }
    }
}

// ============================================================
// TRAINS
// ============================================================

/**
 * A train on the Enoden railway.
 */
data class Train(
    val id: String,
    val name: String,
    val position: WorldPosition,
    val speed: Double,
    val direction: Int = 1, // 1 = outbound, -1 = inbound
    val stations: List<WorldPosition> = emptyList(),
    val stationIndex: Int = 0,
    val isAtStation: Boolean = false,
    val dwellSeconds: Double = 0.0,
    val maxSpeed: Double = 16.0, // ~58 km/h, Enoden's max
    val passengers: Int = 0
)

/**
 * Train system — moves trains along their station routes with
 * realistic stop behavior.
 */
class TrainSystem : WorldSystem {

    private val trains = mutableListOf<Train>()

    val activeTrains: List<Train> get() = trains.toList()

    fun addTrain(train: Train) {
        trains.add(train)
    }

    fun removeTrain(id: String) {
        trains.removeAll { it.id == id }
    }

    override suspend fun onStart(runtime: WorldRuntime) {}

    override suspend fun onStop(runtime: WorldRuntime) {
        trains.clear()
    }

    override suspend fun onUpdate(deltaSeconds: Double, runtime: WorldRuntime) {
        for (i in trains.indices) {
            val train = trains[i]

            if (train.isAtStation) {
                // Dwell at the station, then depart.
                val remaining = train.dwellSeconds - deltaSeconds
                if (remaining <= 0) {
                    trains[i] = train.copy(
                        isAtStation = false,
                        dwellSeconds = 0.0
                    )
                } else {
                    trains[i] = train.copy(dwellSeconds = remaining)
                }
                continue
            }

            if (train.stations.isEmpty()) continue

            val targetStation = train.stations[train.stationIndex]
            val delta = targetStation - train.position
            val distance = delta.horizontalDistanceTo(WorldPosition.Zero)
            val step = train.speed * deltaSeconds

            if (distance <= step) {
                // Arrive at station — dwell.
                val nextIndex = if (train.direction > 0) {
                    (train.stationIndex + 1) % train.stations.size
                } else {
                    (train.stationIndex - 1 + train.stations.size) % train.stations.size
                }
                trains[i] = train.copy(
                    position = targetStation,
                    stationIndex = nextIndex,
                    isAtStation = true,
                    dwellSeconds = 25.0,
                    passengers = (train.passengers + (0..5).random() - 2).coerceAtLeast(0)
                )
            } else {
                val factor = step / distance
                trains[i] = train.copy(position = train.position + delta * factor)
            }
        }
    }
}
