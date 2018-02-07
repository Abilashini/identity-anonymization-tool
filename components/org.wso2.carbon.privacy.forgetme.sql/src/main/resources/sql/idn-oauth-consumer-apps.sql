UPDATE IDN_OAUTH_CONSUMER_APPS
SET USERNAME = `pseudonym`
WHERE USERNAME = `username`
      AND USER_DOMAIN = `user_store_domain`
      AND TENANT_ID = (SELECT UM_ID
                       FROM UM_TENANT
                       WHERE UM_DOMAIN_NAME = `tenant_domain`
                       UNION (SELECT '-1234')
                       ORDER BY UM_ID DESC LIMIT 1)