Feature: Cards routing


Background: 

  * def signInTso1 = callonce read('../common/getToken.feature') { username: 'operator1_fr'}
  * def authTokenTso1 = signInTso1.authToken
  * def signInTso2 = callonce read('../common/getToken.feature') { username: 'operator2_it'}
  * def authTokenTso2 = signInTso2.authToken
  * def signInAdmin = callonce read('../common/getToken.feature') { username: 'admin'}
  * def authTokenAdmin = signInAdmin.authToken


Scenario: Post Card only for group Dispatcher

* def card =
"""
{
	"publisher" : "operator1_fr",
	"processVersion" : "1",
	"process"  :"api_test",
	"processInstanceId" : "process2",
	"state": "messageState",
	"groupRecipients": ["Dispatcher"],
	"severity" : "INFORMATION",
	"startDate" : 1553186770681,
	"summary" : {"key" : "defaultProcess.summary"},
	"title" : {"key" : "defaultProcess.title"},
	"data" : {"message":"a message for group Dispatcher"}
}
"""

  * def perimeter =
"""
{
  "id" : "perimeter",
  "process" : "api_test",
  "stateRights" : [
      {
        "state" : "messageState",
        "right" : "ReceiveAndWrite"
      }
    ]
}
"""

  * def perimeterArray =
"""
[   "perimeter"
]
"""

#Create new perimeter
  Given url opfabUrl + 'users/perimeters'
  And header Authorization = 'Bearer ' + authTokenAdmin
  And request perimeter
  When method post
  Then status 201

#Attach perimeter to group
  Given url opfabUrl + 'users/groups/ReadOnly/perimeters'
  And header Authorization = 'Bearer ' + authTokenAdmin
  And request perimeterArray
  When method patch
  Then status 200

# Push card 
Given url opfabPublishCardUrl + 'cards' 
And header Authorization = 'Bearer ' + authTokenTso1 
And request card  
When method post
Then status 201

#get card with user operator1_fr
Given url opfabUrl + 'cards/cards/api_test.process2' 
And header Authorization = 'Bearer ' + authTokenTso1 
When method get
Then status 200
And match response.card.data.message == 'a message for group Dispatcher'
And def cardUid = response.card.uid


#get card from archives with  user operator1_fr
Given url opfabUrl + 'cards/archives/' + cardUid 
And header Authorization = 'Bearer ' + authTokenTso1 
When method get
Then status 200
And match response.card.data.message == 'a message for group Dispatcher'


#get card with user operator2_fr should not be possible
Given url opfabUrl + 'cards/cards/api_test.process2' 
And header Authorization = 'Bearer ' + authTokenTso2 
When method get
Then status 404


#get card from archives with user operator2_fr should not be possible
Given url opfabUrl + 'cards/archives/' + cardUid 
And header Authorization = 'Bearer ' + authTokenTso2 
When method get
Then status 404



Scenario: Post Card for groups Dispatcher and Planner

* def card =
"""
{
	"publisher" : "operator1_fr",
	"processVersion" : "1",
	"process"  :"api_test",
	"processInstanceId" : "process2tso",
	"state": "messageState",
	"groupRecipients": ["Dispatcher", "Planner"],
	"severity" : "INFORMATION",
	"startDate" : 1553186770681,
	"summary" : {"key" : "defaultProcess.summary"},
	"title" : {"key" : "defaultProcess.title"},
	"data" : {"message":"a message for groups Dispatcher and Planner"}
}
"""

# Push card 
Given url opfabPublishCardUrl + 'cards' 
And header Authorization = 'Bearer ' + authTokenTso1 
And request card  
When method post
Then status 201

#get card with user operator1_fr
Given url opfabUrl + 'cards/cards/api_test.process2tso' 
And header Authorization = 'Bearer ' + authTokenTso1 
When method get
Then status 200
And match response.card.data.message == 'a message for groups Dispatcher and Planner'
And def cardUid = response.card.uid


#get card from archives with user operator1_fr
Given url opfabUrl + 'cards/archives/' + cardUid 
And header Authorization = 'Bearer ' + authTokenTso1 
When method get
Then status 200
And match response.card.data.message == 'a message for groups Dispatcher and Planner'


#get card with user operator2_fr should be possible
Given url opfabUrl + 'cards/cards/api_test.process2tso' 
And header Authorization = 'Bearer ' + authTokenTso2 
When method get
Then status 200
And match response.card.data.message == 'a message for groups Dispatcher and Planner'
And def cardUid = response.card.uid


#get card from archives with user operator2_fr should be possible
Given url opfabUrl + 'cards/archives/' + cardUid 
And header Authorization = 'Bearer ' + authTokenTso2 
When method get
Then status 200
And match response.card.data.message == 'a message for groups Dispatcher and Planner'

#delete perimeter created previously
  Given url opfabUrl + 'users/perimeters/perimeter'
  And header Authorization = 'Bearer ' + authTokenAdmin
  When method delete
  Then status 200
