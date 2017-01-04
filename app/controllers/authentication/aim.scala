package controllers.authentication

import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.{WS, WSResponse}
import play.api.libs.json._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object aim {

  val SERVICE_URLS:Map[String, String] = Map("records" -> "https://api.byu.edu/rest/v1/apikey/academic/records/studentrecord/",
    "schedule" -> "https://ws.byu.edu/rest/v1.0/academic/registration/studentschedule/")
  val VALID_KEY_TYPES = List("API", "WsSession")

  // automated json parsing
  case class Nonce(nonceKey: String, nonceValue: String)
  implicit val nonceReads = Json.reads[Nonce]

  /**
    * Retrieve the body of the nonce which will contain a nonceKey and nonceValue
    */
  private def getNonce(apiKey: String, actor: String): Nonce = {
    val actorPath = if (actor.isEmpty) "" else "/" + actor
    val uri = "https://ws.byu.edu/authentication/services/rest/v1/hmac/nonce/" + apiKey + actorPath
    val request = WS.url(uri).post("").map { response =>
      val body = response.json
      val nonceFromJson: JsResult[Nonce] = Json.fromJson[Nonce](body)

      nonceFromJson match {
        case JsSuccess(n: Nonce, path: JsPath) => {
          Logger.info(s"Got nonce: nonceKey:[$n.nonceKey], nonceValue:[$n.nonceValue]")
          n
        }
        case e: JsError => {
          Logger.error(s"aim:getNonce:Errors: ${JsError.toJson(e).toString}")
          throw new Exception(s"Errors: ${JsError.toJson(e).toString}")
        }
      }
    }
    try {
      // blocks
      Await.result(request, 10 seconds)
    } catch {
      case _: Throwable => {
        Logger.debug("Aim: Getting Nonce timed out")
        throw new Exception("Error getting Nonce")
      }
    }
  }

  /**
    * Produce a base-64 encoded sha512 hmac, as per https://github.com/BYU-ODH/byu-ws/blob/master/src/byu_ws/core.clj#L107
    */
  private def makeSHA512Hmac(sharedSecret: String, itemToEncode: String) = {
    val algorithm = "HmacSHA512"
    val keySpec = new SecretKeySpec(sharedSecret.getBytes("UTF8"), algorithm)
    val macAlgorithm = Mac.getInstance(keySpec.getAlgorithm())
    macAlgorithm.init(keySpec)
    val hmac = macAlgorithm.doFinal(itemToEncode.getBytes("UTF8"))
    val encoder = new org.apache.commons.codec.binary.Base64(0)
    encoder.encodeToString(hmac)
  }

  private def nonceEncode(sharedSecret: String, itemToEncode: String) = makeSHA512Hmac(sharedSecret, itemToEncode)

  /**
    * Get the authorization header necessary for some BYU web services
    */
  private def getHttpAuthorizationHeader(keys: Map[String, String], actorInHash: Boolean): String = {
    keys.get("encoding-type") match {
    case Some("Nonce") => {
        val nonceObj: Nonce = getNonce(keys.getOrElse("api-key", ""), keys.getOrElse("actor", ""))
        val encodedUrl = nonceEncode(keys.getOrElse("shared-secret", ""), nonceObj.nonceValue)
        "Nonce-Encoded-" + keys.get("key-type").getOrElse("API") + "-Key " + keys.get("api-key").getOrElse("") + "," + nonceObj.nonceKey + "," + encodedUrl
      }
      case Some("URL") => "Not-Implemented"
      case _ => throw new Exception("Invalid AIM key-type")
    }
  }

  /**
    * @param service A String: "records" or "schedule"
    * @param param The person id (some number). But if the service is schedule, then it is "$personId/$currentYearTerm"
    * @param netid The person's netid
    * @return Future. In javascript speak this is a promise. Wrapped in an option.
    * You'll have to map the Option and then the Future inside of that.
    */
  def getStudentData(service: String, param: String, netid: String): Option[Future[WSResponse]] = {
    val baseUrl:Option[String] = SERVICE_URLS.get(service)
    if (!baseUrl.isEmpty) {
      var key = ""
      var secret = ""
      var urlParam = param
      if (service == "records" || service == "schedule") {
        key = current.configuration.getString("aim." + service + ".apikey").get
        secret = current.configuration.getString("aim." + service + ".secret").get
      } else {
        throw new Exception(s"Invalid service type: [$service]")
      }
      val serviceUrl = baseUrl.get + urlParam
      val authHeader = getHttpAuthorizationHeader(Map(
        "api-key" -> key,
        "shared-secret" -> secret,
        "key-type" -> "API",
        "encoding-type" -> "Nonce",
        "url" -> serviceUrl,
        "request-body" -> "",
        "actor" -> netid,
        "content-type" -> "application/json",
        "http-method" -> "GET"
      ), true)
      Some(WS.url(serviceUrl).withHeaders("authorization" -> authHeader).get())
    } else {
      None
    }
  }
}
