package com.emarsys.rdb.connector.mysql

import com.emarsys.rdb.connector.mysql.MySqlConnector.MySqlConnectionConfig

object TestHelper {
  val TEST_CONNECTION = MySqlConnectionConfig(
    host = "db",
    port = 3306,
    dbName = "it-test-db",
    dbUser = "it-test-user",
    dbPassword = "it-test-pw",
    certificate = """-----BEGIN CERTIFICATE-----
                    |MIIC8DCCAdgCAQIwDQYJKoZIhvcNAQELBQAwPDE6MDgGA1UEAwwxTXlTUUxfU2Vy
                    |dmVyXzUuNy4xN19BdXRvX0dlbmVyYXRlZF9DQV9DZXJ0aWZpY2F0ZTAeFw0xNzEw
                    |MjYwNzQ0NTZaFw0yNzEwMjQwNzQ0NTZaMEAxPjA8BgNVBAMMNU15U1FMX1NlcnZl
                    |cl81LjcuMTdfQXV0b19HZW5lcmF0ZWRfU2VydmVyX0NlcnRpZmljYXRlMIIBIjAN
                    |BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyiGbFOyftkc/GLr6zRYwinblixgU
                    |EsWeIVP+Q5xv6Of5Q1d4gGhETzbdb0S3IuwWIcAUeY/yLa6XZs2V3b5BiD6OsNA9
                    |0r3xAg8IN9EIbrjgiuyPrgRL6TGyTjHA9qkk/rymMR+Nv7a1TeI8L4owA7adYWy+
                    |Ep4VG3ITRrL/p2KEuWxtOPjdudsV4TqwkZO1EXF59Q0E/3QirKatihkOIBggsKIH
                    |17c5UQBjkObxUd9XmxeDWorH8wfaaXspzG5tD3eFBsE9SUXSxAfJJcTFB9kyFtgq
                    |wYe9m62RVR+CX25nuGvVpx39zo+yHdLxZQJmiAO6ZCIuVxA1y1HRWVQ3uQIDAQAB
                    |MA0GCSqGSIb3DQEBCwUAA4IBAQBZPCh5T4osUKDWD+7v3OTrUBsc1kXLabSXoBYc
                    |SfxNQvIUwJE9EotNOUWgM6c4zyYLj8AxYPj0vOUAefFE5GVRymp/L38PAHedeoX5
                    |+HJdkqhKkiOQDJfvJJv7FYfqg5BsraBC6Hx7LUYIyv2Lgz2gSTUDD5k2a2/FTitF
                    |sD9Ylr9JrNkD5a60ECMej5pGC4Ubzafs4NweTsAsRQVTGQzwK78nUgYu3Vxw3Njv
                    |ZSe7fLr8jk88uQ9E93Q0Z4xyvGsA9NzRpnG6W0RvY2HgPdt++oWljRyFOEAmwtS6
                    |91dGnLTCQLp8kgY3Wj7QLYw5ljAmax8EA61tvVvkZsDGnf5w
                    |-----END CERTIFICATE-----""".stripMargin,
    connectionParams = ""
  )
}
