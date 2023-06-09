Feature: Client ip control

  Background:
   #Getting token for admin and operator1_fr user calling getToken.feature
    * def signIn = callonce read('../common/getToken.feature') { username: 'admin'}
    * def authToken = signIn.authToken
    * def signInAsTSO = callonce read('../common/getToken.feature') { username: 'user_test_api_1'}
    * def authTokenAsTSO = signInAsTSO.authToken

    * def userServiceUrl = opfabUserUrl + 'users/user_test_api_1'
    * def businessConfigServiceUrl = opfabBusinessConfigUrl + 'businessconfig/processes'
    * def cardsConsultationServiceUrl = opfabCardsConsultationUrl + 'cardSubscription' + '?notification=false&clientId=abc0123456789def'
    * def userCardsPublicationServiceUrl = opfabPublishCardUrl + 'cards/userCard'
    * def cardsPublicationServiceUrl = opfabPublishCardUrl + 'cards'

    * def nginxUserServiceUrl = opfabUrl + 'users/users/user_test_api_1'
    * def nginxBusinessConfigServiceUrl = opfabUrl + 'businessconfig/processes'
    * def nginxCardsConsultationServiceUrl = opfabUrl + 'cards/cardSubscription' + '?notification=false&clientId=abc0123456789def'
    * def nginxUserCardsPublicationServiceUrl = opfabUrl + '/cardspub/cards/userCard'

    * def userAuthorizedIPAddressesUpdate =
"""
{
  "login" : "user_test_api_1",
  "entities" : ['ENTITY1_FR'],
  "authorizedIPAddresses" : ['10.0.2.2','10.0.3.3']
}
"""
    * def userEmptyAuthorizedIPAddressesUpdate =
"""
{
  "login" : "user_test_api_1",
  "entities" : ['ENTITY1_FR'],
  "groups" : ["groupIp"],
  "authorizedIPAddresses" : []
}
"""

      * def groupIp =
"""
{
  "id" : "groupIp",
  "name" : "groupIp name",
  "description" : "groupIp description"
}
"""


    * def perimeterIp =
"""
{
  "id" : "perimeterIp",
  "process" : "api_test",
  "stateRights" : [
      {
        "state" : "messageState",
        "right" : "ReceiveAndWrite"
      }
  ]
}
"""

    * def operator1Array =
"""
[   "user_test_api_1"
]
"""
    * def groupArray =
"""
[   "groupIp"
]
"""
    * def card =
"""
{
	"publisher" : "ENTITY1_FR",
	"processVersion" : "1",
	"process"  :"api_test",
	"processInstanceId" : "initialCardProcess",
	"state": "messageState",
	"entityRecipients" : ["ENTITY1_FR"],
	"severity" : "INFORMATION",
	"startDate" : 1553186770681,
	"summary" : {"key" : "defaultProcess.summary"},
	"title" : {"key" : "defaultProcess.title"},
	"data" : {"message":"a message"}
}

"""

  * def card1 =
  """
  {
    "publisher" : "user_test_api_1",
    "processVersion" : "1",
    "process"  :"api_test",
    "processInstanceId" : "initialCardProcess1",
    "state": "messageState",
    "entityRecipients" : ["ENTITY1_FR"],
    "severity" : "INFORMATION",
    "startDate" : 1553186770681,
    "summary" : {"key" : "defaultProcess.summary"},
    "title" : {"key" : "defaultProcess.title"},
    "data" : {"message":"a message"}
  }
  
  """

  Scenario: create or update user with 2 authorized ip addresses

    Given url opfabUrl + 'users/users/user_test_api_1'
    And header Authorization = 'Bearer ' + authToken
    And request userAuthorizedIPAddressesUpdate
    When method put
    Then assert responseStatus == 200 || responseStatus == 201
    And match response.login == userAuthorizedIPAddressesUpdate.login
    And match response.authorizedIPAddresses contains '10.0.2.2'
    And match response.authorizedIPAddresses contains '10.0.3.3'


  Scenario: Create groupIp
    Given url opfabUrl + 'users/groups'
    And header Authorization = 'Bearer ' + authToken
    And request groupIp
    When method post
    Then match response.description == groupIp.description
    And match response.name == groupIp.name
    And match response.id == groupIp.id


  Scenario: Add user_test_api_1 to groupIp
    Given url opfabUrl + 'users/groups/' + groupIp.id + '/users'
    And header Authorization = 'Bearer ' + authToken
    And request operator1Array
    When method patch
    And status 200


  Scenario: Create perimeterIp
    Given url opfabUrl + 'users/perimeters'
    And header Authorization = 'Bearer ' + authToken
    And request perimeterIp
    When method post


  Scenario: Put groupIp for perimeterIp
    Given url opfabUrl + 'users/perimeters/'+ perimeterIp.id + '/groups'
    And header Authorization = 'Bearer ' + authToken
    And request groupArray
    When method put
    Then status 200




  Scenario Outline: Check direct calls from unauthorized ip are refused

    Given url <url>
    And header Authorization = 'Bearer ' + authTokenAsTSO
    And header X-Forwarded-For = '10.0.1.1'
    And request <request>
    When method <method>
    Then status <expected>

    Examples:
    | url                          | method | request | expected  |
    | userServiceUrl               | get    | ''      | 403       |
    | businessConfigServiceUrl     | get    | ''      | 200       |
    | userCardsPublicationServiceUrl   | post   | card    | 403       |
    | cardsPublicationServiceUrl   | post   | card1    | 403       |
    | cardsConsultationServiceUrl  | get    | ''      | 403       |


Scenario Outline: Check direct calls from authorized ip are accepted

    Given url <url>
    And header Authorization = 'Bearer ' + authTokenAsTSO
    And header X-Forwarded-For = '10.0.2.2'
    And request <request>
    When method <method>
    Then status <expected>

    Examples:
    | url                          | method | request | expected  |
    | userServiceUrl               | get    | ''      | 200       |
    | businessConfigServiceUrl     | get    | ''      | 200       |
    | userCardsPublicationServiceUrl   | post   | card    | 201       |
    | cardsPublicationServiceUrl   | post   | card1   | 201       |
    | cardsConsultationServiceUrl  | get    | ''      | 200       |


  Scenario Outline: Check calls through nginx from unauthorized ip are refused 

    Given url <url>
    And header Authorization = 'Bearer ' + authTokenAsTSO
    And header X-Forwarded-For = '10.0.2.2'
    And request <request>
    When method <method>
    Then status <expected>

    Examples:
    | url                                   | method | request | expected  |
    | nginxUserServiceUrl                   | get    | ''      | 403       |
    | nginxBusinessConfigServiceUrl         | get    | ''      | 200       |
    | nginxUserCardsPublicationServiceUrl   | post   | card    | 403       |
    | nginxCardsConsultationServiceUrl      | get    | ''      | 403       |



  Scenario: update an existing user with empty authorizedIPAddresses

    Given url opfabUrl + 'users/users/user_test_api_1'
    And header Authorization = 'Bearer ' + authToken
    And request userEmptyAuthorizedIPAddressesUpdate
    When method put
    Then status 200
    And match response.login == userAuthorizedIPAddressesUpdate.login
    And match response.authorizedIPAddresses == []


Scenario Outline: Check direct calls are accepted

    Given url <url>
    And header Authorization = 'Bearer ' + authTokenAsTSO
    And request <request>
    When method <method>
    Then status <expected>

    Examples:
    | url                               | method | request | expected  |
    | userServiceUrl                    | get    | ''      | 200       |
    | businessConfigServiceUrl          | get    | ''      | 200       |
    | userCardsPublicationServiceUrl    | post   | card    | 201       |
    | cardsPublicationServiceUrl        | post   | card1   | 201       |
    | cardsConsultationServiceUrl       | get    | ''      | 200       |


  Scenario Outline: Check calls through nginx are accepted

    Given url <url>
    And header Authorization = 'Bearer ' + authTokenAsTSO
    And request <request>
    When method <method>
    Then status <expected>

    Examples:
    | url                                   | method | request | expected  |
    | nginxUserServiceUrl                   | get    | ''      | 200       |
    | nginxBusinessConfigServiceUrl         | get    | ''      | 200       |
    | nginxUserCardsPublicationServiceUrl   | post   | card    | 201       |
    | nginxCardsConsultationServiceUrl      | get    | ''      | 200       |


    Scenario: delete user card, expected response 200
      Given url opfabPublishCardUrl + 'cards/userCard/api_test.initialCardProcess'
      And header Authorization = 'Bearer ' + authTokenAsTSO
      When method delete
      Then status 200


  Scenario: delete the perimeter previously created
    Given url opfabUrl + 'users/perimeters/perimeterIp'
    And header Authorization = 'Bearer ' + authToken
    When method delete
    Then status 200


  Scenario: delete the group previously created
    Given url opfabUrl + 'users/groups/groupIp'
    And header Authorization = 'Bearer ' + authToken
    When method delete
    Then status 200

    Scenario: delete test user
      Given url opfabUrl + "users/users/user_test_api_1"
      And header Authorization = "Bearer " + authToken
      When method delete
      Then status 200