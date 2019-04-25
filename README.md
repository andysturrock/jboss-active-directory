# jboss-active-directory
Demo of Active Directory authentication and authorisation

## Credits and acknowledgements
A lot of the information in this demo came from:
* https://access.redhat.com/documentation/en/red-hat-jboss-enterprise-application-platform/version-6.4/how-to-setup-sso-with-kerberos
* https://github.com/kwart/spnego-demo


## Demo variables
The demo uses the following domains/realms/hosts:
* Kerberos realm:			AD1.STURROCK.ORG
* AD domain:				ad1.sturrock.org
* JBoss server hostname:		linux-srv1
* Active Directory server hostname:	ad-svr1.ad1.sturrock.org
* JBoss server username:		jboss-user

## Configure JBoss user in AD
1. Create JBoss server user account in Active Directory
	1. Use the "Active Directory Users and Computers" tool to create a user
	2. Tick "User cannot change password" on Account tab
	3. Tick "Password never expires" on Account tab
	4. Contrary to instructions on other sites, DO NOT tick "Do not require Kerberos preauthentication" on Account tab.  If you tick this option the server cannot perform an ldap search.

2. Map the JBoss server user account to a Service Principal Name (SPN) and export the keytab file:

	```ktpass /princ HTTP/linux-srv1.ad1.sturrock.org@AD1.STURROCK.ORG /mapuser jboss-user@AD1.STURROCK.ORG +rndpass /crypto All /ptype KRB5_NT_PRINCIPAL /out jboss-user.keytab```
	
3. Copy the `jboss-user.keytab` file to the linux-srv1 host, into the JBoss server config directory (probably ```standalone/configuration``` under the JBoss installation directory).

## Configure JBoss server
1.  If the linux-svr1 host has been joined to the Active Directory domain, there should be a correctly configured krb5.conf file in /etc.  If not, create one in the JBoss directory.  It should look like:

```Shell
[logging]
                default = FILE:/var/log/krb5libs.log
                kdc = FILE:/var/log/krb5kdc.log
                admin_server = FILE:/var/log/kadmind.log
[libdefaults]
                canonicalize = true
                default_realm = AD1.STURROCK.ORG
                dns_lookup_realm = true
                dns_lookup_kdc = true
                ticket_lifetime = 24h
                renew_lifetime = 7d
                forwardable = true
                rdns = false
```
2. Configure the server identity security domain.  Use the following commands in jboss-cli.sh:

```Shell
/subsystem=security/security-domain=host:add(cache-type=default)

/subsystem=security/security-domain=host/authentication=classic:add

/subsystem=security/security-domain=host/authentication=classic/login-module=Kerberos:add( \
  code=Kerberos, \
  flag=required, \
  module-options=[ \
    ("storeKey"=>"true"), \
    ("refreshKrb5Config"=>"true"), \
    ("useKeyTab"=>"true"), \
  ("principal"=>"HTTP/linux-srv1.ad1.sturrock.org@AD1.STURROCK.ORG"), \
    ("keyTab"=>"${jboss.server.config.dir}/jboss-user.keytab"), \
    ("doNotPrompt"=>"true"), \
    ("debug"=>"false") \
  ])

reload
```

3. Configure the web application security domain.  Use the following commands in jboss-cli.sh:

```Shell
/subsystem=security/security-domain=SPNEGO:add(cache-type=default)
 
/subsystem=security/security-domain=SPNEGO/authentication=classic:add
 
/subsystem=security/security-domain=SPNEGO/authentication=classic/login-module=SPNEGO:add( \
  code=SPNEGO, \
  flag=required, \
  module-options=[ \
    ("password-stacking"=>"useFirstPass"), \
    ("serverSecurityDomain"=>"host") \
  ])

reload
```
4. Add the following to the JBoss config file (probably standalone.xml) after the extensions section:

```XML
<system-properties>
  <property name="java.security.krb5.kdc" value="ad1.sturrock.org"/>
  <property name="java.security.krb5.realm" value="AD1.STURROCK.ORG"/>
  <property name="java.security.krb5.conf" value="/etc/krb5.conf"/>
  <property name="java.security.krb5.debug" value="true"/>
</system-properties>
```
5. Configure the LDAP role mapping.  Use the following commands in jboss-cli.sh:

```Shell
/subsystem=security/security-domain=SPNEGO/authentication=classic/login-module=AdvancedAdLdap:add( \
  code=AdvancedAdLdap, \
  flag=required, \
  module-options=[ \
    ("password-stacking"=>"useFirstPass"), \
    ("bindAuthentication"=>"GSSAPI"), \
  ("jaasSecurityDomain"=>"host"), \
    ("java.naming.provider.url"=>"ldap://ad-svr1.ad1.sturrock.org:389"), \
    ("baseCtxDN"=>"CN=Users,DC=ad1,DC=sturrock,DC=org"), \
    ("baseFilter"=>"(userPrincipalName={0})"), \
    ("rolesCtxDN"=>"CN=Users,DC=ad1,DC=sturrock,DC=org"), \
    ("roleFilter"=>"(distinguishedName={1})"), \
    ("roleAttributeID"=>"memberOf"), \
    ("roleAttributeIsDN"=>"true"), \
    ("roleNameAttributeID"=>"cn"), \
    ("recurseRoles"=>"true") \
  ])
```
Note you must specify the actual hostname of the AD server for the ldap URL.  This is because Kerberos and LDAP use lots of reverse DNS, which won't resolve properly if you use the domain name.

## Run and test
1. Run the JBoss server
 
2. Build and deploy the demo app:

```Shell
mvn clean jboss-as:deploy
```

3. Call some of the methods.  The easiest way to do this is use `curl` from the command line:

```Shell
curl --negotiate -X GET -u : http://linux-svr1.ad1.sturrock.org:8080/jboss-active-directory/rest/protected-get
```

4. This initial test should fail as the user won't be in the right group.  Add the user to the `ProtectedRole` group and retest.

## Debugging
Listed below are several ways to debug the setup which helped me finally get this working:

1. Turn on debugging in the standalone.xml by setting/adding the following elements:

```xml
<system-properties>
	...
	<property name="java.security.krb5.debug" value="true"/>
	...
</system-properties>
```

```xml
<login-module code="Kerberos" flag="required">
	...
	<module-option name="debug" value="true"/>
	...
</login-module>
```

```xml
<subsystem xmlns="urn:jboss:domain:logging:1.5">
	<console-handler name="CONSOLE">
		<level name="TRACE"/>
                <formatter>
			<named-formatter name="COLOR-PATTERN"/>
                </formatter>
	</console-handler>
	...
	<logger category="org.jboss.security">
		<level name="DEBUG"/>
	</logger>
	<logger category="org.jboss.security.negotiation">
		<level name="TRACE"/>
	</logger>
	...
</subsystem>
```

2. Check that the principal in the keytab file works and can perform an ldapsearch:

```Shell
$ kinit -k -t standalone/configuration/jboss-user.keytab HTTP/linux-srv1.ad1.sturrock.org@AD1.STURROCK.ORG
$ klist -e
Ticket cache: FILE:/tmp/krb5cc_1000
Default principal: jboss-user@AD1.STURROCK.ORG

Valid starting       Expires              Service principal
02/04/2016 06:45:11  02/04/2016 16:45:11  krbtgt/AD1.STURROCK.ORG@AD1.STURROCK.ORG
        renew until 02/11/2016 06:45:11, Etype (skey, tkt): aes256-cts-hmac-sha1-96, aes256-cts-hmac-sha1-96
$ ldapsearch -H ldap://ad-svr1.ad1.sturrock.org:389 -b CN=Users,DC=ad1,DC=sturrock,DC=org
SASL/GSSAPI authentication started
SASL username: jboss-user@AD1.STURROCK.ORG
SASL SSF: 56
SASL data security layer installed.
# extended LDIF
#
# LDAPv3
# base <CN=Users,DC=ad1,DC=sturrock,DC=org> with scope subtree
# filter: (objectclass=*)
# requesting: ALL
#

# Users, ad1.sturrock.org
dn: CN=Users,DC=ad1,DC=sturrock,DC=org
objectClass: top
objectClass: container
cn: Users
description: Default container for upgraded user accounts
distinguishedName: CN=Users,DC=ad1,DC=sturrock,DC=org
instanceType: 4
whenCreated: 20151014085423.0Z
whenChanged: 20151014085423.0Z
uSNCreated: 5808
uSNChanged: 5808
showInAdvancedViewOnly: FALSE
name: Users
objectGUID:: 3UWN11k4nkCxQuk9i/XcjQ==
systemFlags: -1946157056
objectCategory: CN=Container,CN=Schema,CN=Configuration,DC=ad1,DC=sturrock,DC=
 org
isCriticalSystemObject: TRUE
dSCorePropagationData: 20151014085608.0Z
dSCorePropagationData: 16010101000001.0Z
...
```
