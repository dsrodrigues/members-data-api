# Members' Data API

The members' data API is a Play app that manages and retrieves supporter attributes associated with a user.  
It runs on https://members-data-api.theguardian.com/.

## Setting it up locally

1. You will need to have [dev-nginx](https://github.com/guardian/dev-nginx) installed.

1. Follow the [nginx steps for identity-platform](https://github.com/guardian/identity-platform/blob/master/nginx/README.md#setup-nginx-with-ssl-for-dev).

1. Follow the [identity-frontend configuration steps](https://github.com/guardian/identity-frontend#configuration).

1. Then run `./setup.sh` in `nginx/`.

1. Add the following entries to your hosts file:  
```
127.0.0.1   profile.thegulocal.com
127.0.0.1   members-data-api.thegulocal.com
```

1. Get Janus credentials for membership.

1. Download the config  
(you may need to `brew install awscli` to get the command.)  
`aws s3 cp s3://gu-reader-revenue-private/membership/members-data-api/DEV/members-data-api.private.conf /etc/gu/ --profile membership`

## Running Locally

1. Get Janus credentials for membership.

1. Create an ssh tunnel to the CODE one-off contributions database:
    1. Clone https://github.com/guardian/contributions-platform
    2. From the contributions-platform project, Run `./contributions-store/contributions-store-bastion/scripts/open_ssh_tunnel.sh -s CODE` (requires [marauder](https://github.com/guardian/prism/tree/master/marauder))

1. Ensure an `nginx` service is running locally.

1. To start the Members' data API service run `./start-api.sh`.  
The service will be running on 9400 and use the SupporterAttributesFallback-DEV DynamoDB table.

1. go to https://members-data-api.thegulocal.com/user-attributes/me/mma-membership.  
If you get a 401 response, it probably means your Identity credentials have expired.  
Renew them by:
    1. Start up a local Identity service by running script `start-frontend.sh` in the `identity-frontend` repo.
    1. Go to https://profile.thegulocal.com/signin.

## Running tests

run sbt and then test.  It will download a dynamodb table from S3 and use that.  Tip: watch out for firewalls blocking the download, you may need to turn them off to stop it scanning the file.

## Testing manually

A good strategy for testing your stuff is to run a local identity-frontend, membership-frontend and members-data-api.  Then sign up for membership and hit the above url, which should return the right JSON structure.

The /me endpoints use the GU_U and SC_GU_U from the Cookie request header.

### Identity Frontend

Identity frontend is split between [new (profile-origin)](https://github.com/guardian/identity-frontend) and old (profile), which is the identity project in [frontend](https://github.com/guardian/frontend). Only profile uses the membership-attribute-service. Make sure that it's pointing at your local instance.

    devOverrides{
             guardian.page.userAttributesApiUrl="https://members-data-api.thegulocal.com/user-attributes"
             id.members-data-api.url="https://members-data-api.thegulocal.com/"
    }
 
## API Docs

The SupporterAttributesFallback Dynamo table has identity id as a primary key. Corresponding to each identity id in the table 
we have information about that user's membership, subscriptions, and/or digital pack. 

On each lookup call (i.e. /user-attributes/{me}), we derive this information from subscriptions via Zuora, 
and then update the entry if it's out of date. If we can't get subscriptions from Zuora, we fall back to the 
SupporterAttributesFallback table. There is a TTL on the SupporterAttributesFallback table. 

### GET /user-attributes/me

#### User is a member

    {
        "userId": "xxxx",
        "tier": "Supporter",
        "membershipNumber": "1234",
        "membershipJoinDate": "2017-06-26",
        "contentAccess": {
            "member": true,
            "paidMember": true,
            "recurringContributor": false,
            "digitalPack": false

        }
    }

#### User is a contributor and not a member 
    
    {
        "userId":"xxxx",
        "recurringContributionPaymentPlan":"Monthly Contribution",
        "contentAccess": {
            "member":false,
            "paidMember":false,
            "recurringContributor":true,
            "digitalPack": false

        }
    }


#### User is not a member and not a contributor
    
    {
        "message":"Not found",
        "details":"Could not find user in the database",
        "statusCode":404
    }


#### User is a member and a contributor

    {
        "userId": "xxxx",
        "tier": "Supporter",
        "membershipNumber": "324154",
        "recurringContributionPaymentPlan": "Monthly Contribution",
        "membershipJoinDate": "2017-06-26",
        "contentAccess": {
            "member": true,
            "paidMember": true,
            "recurringContributor": true,
            "digitalPack": false

        }
    }
    
#### User has a digital pack only

    {
        "userId": "30000549",
        "digitalSubscriptionExpiryDate": "2018-11-29",
        "contentAccess": {
            "member": false,
            "paidMember": false,
            "recurringContributor": false,
            "digitalPack": true
        }
    }


### GET /user-attributes/me/membership


#### User is a member

    {
        "userId": "xxxx",
        "tier": "Supporter",
        "membershipNumber": "1234",
        "contentAccess": {
            "member": true,
            "paidMember": true
         }
    }

#### User is a contributor and not a member 

    {
        "message":"Not found",
        "details":"User was found but they are not a member",
        "statusCode":404
    }


#### User is a member and contributor

    {
        "userId": "xxxx",
        "tier": "Supporter",
        "membershipNumber": "1234",
        "contentAccess": {
            "member": true,
            "paidMember": true
        }
    }


#### User is not a member and not a contributor

    {
        "message":"Not found",
        "details":"Could not find user in the database",
        "statusCode":404
    }


### GET /user-attributes/me/features
Responses:

    {
      "adFree": true,
      "adblockMessage": false,
      "userId": "123",
      "membershipJoinDate": "2017-04-04"
    }
