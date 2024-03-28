package prism

import org.joda.time.DateTime
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AnyFreeSpec

class RecommendationsTest extends AnyFreeSpec with Matchers with OptionValues {
  import util.Fixtures._
  import Recommendations._

  "newerThan" - {
    val now = DateTime.now()
    val oldAmi = emptyAmi("old").copy(creationDate = Some(now.minusDays(10)))
    val youngAmi = emptyAmi("young").copy(creationDate = Some(now.minusDays(2)))
    val noDateAmi = emptyAmi("no-date")

    "returns true if AMI is newer" in {
      newerThan(oldAmi)(youngAmi) shouldEqual true
    }

    "returns false if AMI is older" in {
      newerThan(youngAmi)(oldAmi) shouldEqual false
    }

    "returns false if source AMI doesn't have a date" in {
      newerThan(noDateAmi)(youngAmi) shouldEqual false
    }

    "returns false if candidate AMI doesn't have a date" in {
      newerThan(oldAmi)(noDateAmi) shouldEqual false
    }
  }

  "isObsoleteUbuntu" - {
    "allows a recent LTS dist" in {
      val imageName =
        "ubuntu/images/hvm-ssd/ubuntu-trusty-14.04-amd64-server-20160222"
      isObsoleteUbuntu(
        emptyAmi("arn").copy(name = Some(imageName)),
        now = DateTime.parse("2014-07-15")
      ) shouldEqual false
    }

    "allows supported legacy LTS dist" in {
      val imageName =
        "ubuntu/images/hvm-ssd/ubuntu-precise-12.04-amd64-server-20160315"
      isObsoleteUbuntu(
        emptyAmi("arn").copy(name = Some(imageName)),
        now = DateTime.parse("2015-08-15")
      ) shouldEqual false
    }

    "allows current non-LTS" in {
      val imageName =
        "ubuntu/images/hvm-ssd/ubuntu-wily-15.10-amd64-server-20160315"
      isObsoleteUbuntu(
        emptyAmi("arn").copy(name = Some(imageName)),
        now = DateTime.parse("2016-01-27")
      ) shouldEqual false
    }

    "allows non-understood name" in {
      isObsoleteUbuntu(
        emptyAmi("arn").copy(name = Some("not-a-good-name")),
        now = DateTime.parse("2016-01-27")
      ) shouldEqual false
    }

    "allows not-understood dist" in {
      val imageName =
        "ubuntu/images/hvm-ssd/ubuntu-new-99.99-amd64-server-20160315"
      isObsoleteUbuntu(
        emptyAmi("arn").copy(name = Some(imageName)),
        now = DateTime.parse("2016-01-27")
      ) shouldEqual false
    }

    "warns about an out-of date ARN" in {
      val imageName =
        "ubuntu/images/hvm-ssd/ubuntu-saucy-13.10-amd64-server-20140709"
      isObsoleteUbuntu(
        emptyAmi("arn").copy(name = Some(imageName)),
        now = DateTime.parse("2016-01-27")
      ) shouldEqual true
    }

    "returns false for a non-Ubuntu AMI" in {
      isObsoleteUbuntu(
        emptyAmi("arn"),
        DateTime.parse("2024-02-27")
      ) shouldEqual false
    }

    "date edge-cases" - {
      "for an LTS release (even number year and '04' month e.g. '24.04')" - {
        val imageName =
          "ubuntu/images/hvm-ssd/ubuntu-trusty-14.04-amd64-server-20160222"

        "returns false if we are less than 5 years after release" in {
          isObsoleteUbuntu(
            emptyAmi("arn").copy(name = Some(imageName)),
            now = DateTime.parse("2019-03-31T23:59")
          ) shouldEqual false
        }

        "returns true if we are more than 5 years after release" in {
          isObsoleteUbuntu(
            emptyAmi("arn").copy(name = Some(imageName)),
            now = DateTime.parse("2019-04-01T08:00")
          ) shouldEqual true
        }
      }

      "for a non-LTS release" - {
        val imageName =
          "ubuntu/images/hvm-ssd/ubuntu-wily-15.10-amd64-server-20160315"

        "returns false if we are less than 9 months after release" in {
          isObsoleteUbuntu(
            emptyAmi("arn").copy(name = Some(imageName)),
            now = DateTime.parse("2016-06-30T23:59")
          ) shouldEqual false
        }

        "returns true if we are more than 9 months after release" in {
          isObsoleteUbuntu(
            emptyAmi("arn").copy(name = Some(imageName)),
            now = DateTime.parse("2016-07-01T08:00")
          ) shouldEqual true
        }
      }
    }
  }
}
