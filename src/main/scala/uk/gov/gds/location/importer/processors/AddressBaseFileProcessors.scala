package uk.gov.gds.location.importer.processors

import uk.gov.gds.location.importer.logging._
import java.io.File
import org.joda.time.DateTime
import uk.gov.gds.location.importer.mongo.MongoConnection
import Extractors._
import uk.gov.gds.location.importer.conversions.AddressBaseToLocateConvertor
import AddressBaseToLocateConvertor.toLocateAddress
import uk.gov.gds.location.importer.model._
import uk.gov.gds.location.importer.model.StreetWithDescription

/**
 * Methods in this class take a file and attempt to process it into a locate style object
 * Can process:
 *    (1) Code Point files
 *    (2) AddressBase files for streets
 *    (3) AddressBase files for addresses
 * (2) must be done prior to (3)
 * @param mongoConnection
 */
class AddressBaseFileProcessors(mongoConnection: MongoConnection) extends Logging  {

  /**
   * Process code point files
   *
   * @param file
   * @return Boolean file processed successfully
   */
  def processCodePointFile(file: File) = {
    logger.info("Processing codepoint: " + file.getName)
    val start = new DateTime()

    try {
      persistCodePoint(processRowsIntoCodePoint(file))
      logger.info(String.format("Successfully processed %s in %s", file.getName, (new DateTime().getMillis - start.getMillis).toString))
      true
    } catch {
      case e: Exception => {
        logger.error(String.format("Failed to process %s in %s", file.getName, (new DateTime().getMillis - start.getMillis).toString), e)
        false
      }
    }
  }

  /**
   * Process an address base file, extracting and persisting only street data
   *
   * @param file
   * @return Boolean file processed successfully
   */
  def processAddressBaseForStreets(file: File) = {
    implicit val fileName = file.getName
    logger.info("Processing streets in: " + fileName)

    val start = new DateTime()
    try {
      persistStreetDescriptors(processRowsIntoStreets(file))
      logger.info(String.format("Successfully processed %s in %s", file.getName, (new DateTime().getMillis - start.getMillis).toString))
      true
    } catch {
      case e: Exception => {
        logger.error(String.format("Failed to process %s in %s", file.getName, (new DateTime().getMillis - start.getMillis).toString), e)
        false
      }
    }
  }

  /**
   * Process an address base file, extracting locate style addresses.
   * Requires processAddressBaseForStreets to have been completed for all files
   *
   * @param file
   * @return Boolean file processed successfully
   */
  def processAddressBaseForAddresses(file: File) = {
    implicit val fileName = file.getName
    logger.info("Processing addresses in: " + fileName)

    val start = new DateTime()
    try {
      persistAddresses(processRowsIntoAddressWrappers(file), fileName)
      logger.info(String.format("Successfully processed %s in %s", file.getName, (new DateTime().getMillis - start.getMillis).toString))
      true
    } catch {
      case e: Exception => {
        logger.error(String.format("Failed to process %s in %s", file.getName, (new DateTime().getMillis - start.getMillis).toString), e)
        false
      }
    }
  }


  /**
   * Persists the codePoint objects into mongo
   * @param codePoints List of codePoint objects to bulk insert
   */
  private def persistCodePoint(codePoints: List[CodePoint]) {
    AllTheCodePoints.add(codePoints)
    mongoConnection.insertCodePoints(codePoints.map(_.serialize))
  }

  /**
   * Persists the street descriptor objects into mongo
   * @param streetDescriptors List of streetDescriptors objects to bulk insert
   */
  private def persistStreetDescriptors(streetDescriptors: List[StreetWithDescription]) {
    AllTheStreets.add(streetDescriptors)
    mongoConnection.insertStreets(streetDescriptors.map(_.serialize))
  }

  /**
   * Persists the address wrappers into mongo
   * @param addressBaseWrappers List of addressBaseWrappers objects to bulk insert
   */
  private def persistAddresses(addressBaseWrappers: List[AddressBaseWrapper], filename: String) {
    mongoConnection.insertAddresses(addressBaseWrappers.flatMap(toLocateAddress(_, filename)).par.map(_.serialize).toList)
  }

}

