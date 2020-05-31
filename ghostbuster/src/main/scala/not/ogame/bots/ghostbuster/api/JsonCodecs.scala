package not.ogame.bots.ghostbuster.api

import io.circe.{Decoder, Encoder, HCursor, Json, KeyDecoder, KeyEncoder}
import not.ogame.bots.{Coordinates, CoordinatesType, FacilityBuilding, FleetAttitude, FleetMissionType, ShipType, SuppliesBuilding}

trait JsonCodecs {
  implicit val coordinatesTypeEncoder: Encoder[CoordinatesType] = (a: CoordinatesType) => Json.fromString(a.entryName)
  implicit val coordinatesTypeDecoder: Decoder[CoordinatesType] = (c: HCursor) =>
    c.as[String].map { text =>
      CoordinatesType.values.collectFirst { case v if v.entryName == text => v }.get
    }

  implicit val fleetMissionTypeEncoder: Encoder[FleetMissionType] = (a: FleetMissionType) => Json.fromString(a.entryName)
  implicit val fleetMissionTypeDecoder: Decoder[FleetMissionType] = (c: HCursor) =>
    c.as[String].map { text =>
      FleetMissionType.values.collectFirst { case v if v.entryName == text => v }.get
    }

  implicit val fleetAttitudeEncoder: Encoder[FleetAttitude] = (a: FleetAttitude) => Json.fromString(a.entryName)
  implicit val fleetAttitudeDecoder: Decoder[FleetAttitude] = (c: HCursor) =>
    c.as[String].map { text =>
      FleetAttitude.values.collectFirst { case v if v.entryName == text => v }.get
    }

  implicit val shipTypeKeyEncoder: KeyEncoder[ShipType] = (key: ShipType) => key.entryName
  implicit val shipTypeKeyDecoder: KeyDecoder[ShipType] = (key: String) =>
    ShipType.values.collectFirst { case v if v.entryName == key => v }

  implicit val facilityBuildingKeyEncoder: KeyEncoder[FacilityBuilding] = (key: FacilityBuilding) => key.entryName
  implicit val facilityBuildingKeyDecoder: KeyDecoder[FacilityBuilding] = (key: String) =>
    FacilityBuilding.values.collectFirst { case v if v.entryName == key => v }

  implicit val suppliesBuildingKeyEncoder: KeyEncoder[SuppliesBuilding] = (key: SuppliesBuilding) => key.entryName
  implicit val suppliesBuildingKeyDecoder: KeyDecoder[SuppliesBuilding] = (key: String) =>
    SuppliesBuilding.values.collectFirst { case v if v.entryName == key => v }

  implicit val coordsEncoderKeyEncoder: KeyEncoder[Coordinates] = (key: Coordinates) => s"${key.galaxy}:${key.system}:${key.position}"
  implicit val coordsDecoderKeyDecoder: KeyDecoder[Coordinates] = (key: String) => {
    key.split(":").toList match {
      case a :: b :: c :: Nil => Some(Coordinates(a.toInt, b.toInt, c.toInt))
      case _                  => None
    }
  }
}
