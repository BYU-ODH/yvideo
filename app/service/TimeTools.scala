package service

import org.joda.time.format.{DateTimeFormat, DateTimeFormatter, ISODateTimeFormat}
import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * Tools for dealing with string dates.
 */
object TimeTools {

  /**
   * Returns the current date and time as an RFC 3339 formatted date
   * @return The current date and time
   */
  def now(): String = ISODateTimeFormat.dateTime().print(new DateTime())

  /**
   * Takes an RFC 3339 formatted date and formats it as something readable for display on pages.
   * @param time The formatted date
   * @return The easy-to-read date
   */
  def prettyTime(time: String): String = {
    val dateTime = new DateTime(time)
    dateTime.toString("MMM dd, yyyy") + " at " + dateTime.toString("hh:mm aa")
  }

  /**
   * This takes a date formatted somehow, parses it, and converts it to a RFC 3339 formatted date
   * @param date The date to format
   * @return The formatted date
   */
  def parseDate(date: String): String = {
    val dateTime = DateTimeFormat.forPattern("MM/dd/yy").parseDateTime(date)
    ISODateTimeFormat.dateTime().print(dateTime)
  }

  def dateToTimestamp(date: String): Long = new DateTime(date).getMillis
  def stringToDateTime(date: String): DateTime = new DateTime(date)

  /**
   * This takes a date string and checks to see if it is older than the given amount of months
   * @param date The date to check against
   * @return true for expired, false for not
   */
  def checkExpired(date: String): Boolean = {
    val expirationDate = new DateTime().minusMonths(18)
    val givenDate = new DateTime(date)
    expirationDate.isAfter(givenDate)
  }

  /**
   * Returns true if the time between date1 and date2 is longer than m milliseconds
   * @param date1 The start time
   * @param date2 The end time
   * @param m The duration to compare against in milliseconds
   */
  def timeSpanReached(date1: DateTime, date2: DateTime, m: Long): Boolean =
    new Duration(date1, date2).isLongerThan(new Duration(m))

  /**
   * Returns true if more than m milliseconds has passed since the provided date
   * @param date The start time
   * @param m Duration in milliseconds
   */
  def timeHasElapsed(date: DateTime, m: Long): Boolean = timeSpanReached(date, DateTime.now(), m)

  /*
   * @param a DateTime
   * @param b DateTime
   * @return The latest DateTime
   */
  val later = (a: DateTime, b: DateTime) => if (a.isAfter(b)) a else b

  /**
   * Gets the latest datetime from a list of datetime
   * @param dates A list of unsorted datetime
   * @return The latest datetime object
   */
  def getLatest(dates: List[DateTime]): DateTime = dates.fold(new DateTime(0))(later)
}
