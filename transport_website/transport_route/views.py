from django.http import HttpResponse, HttpResponseRedirect
from django.shortcuts import render, get_object_or_404
from transport_route.models import *
from json import *
import datetime
import urllib2, urllib
import parser
import codecs
import uuid
from additional_functions import *

# Reittiopas API
reittiopas_api_url = 'http://api.reittiopas.fi/hsl/prod/'
reittiopas_api_user = 'varnolen2'
reittiopas_api_pass = 'nafpass'

############################################################################################################################

def index(request):
    # Show a resume of the REST API
    return render(request, 'transport_route/index.html', None)

############################################################################################################################

def register_user(request):
    # Retrieve parameters from the request
    name = request.GET.get('name', None)
    username = request.GET.get('username', None)
    password = request.GET.get('pass', None)

    ###########################
    # Create a user in the DB #
    ###########################
    response_data = {}
    # Check if the user nickname already exists
    user = ExtendedUser.objects.filter(username=username)
    if len(user)==0:
        # If not, create a new one
        ############################
        # Create user
        user = ExtendedUser.objects.create_user(username, None, None, first_name=name)
        user.set_password(password)
        user.save()
    else:
        # Return error in the response
        response_data['status'] = 'username already exists'
        # Return the JSON object
        return HttpResponse(dumps(response_data), content_type="application/json")
    
    response_data['status'] = 'user created'
    return HttpResponse(dumps(response_data), content_type="application/json")

############################################################################################################################

def login(request):
    # Retrieve parameters from the request
    username = request.GET.get('username', None)
    password = request.GET.get('pass', None)

    response_data = {}
    # Check if the user nickname already exists
    user = ExtendedUser.objects.filter(username=username)
    if len(user)==0:
        # Return error in the response
        response_data['status'] = 'user do not exists'
        # Return the JSON object
        return HttpResponse(dumps(response_data), content_type="application/json")
    else:
        user = user[0]
        # User found, check whether the password is correct
        if (user.check_password(password)):
            # Create a new access_token (21 length)
            # Code extract from: http://stackoverflow.com/questions/621649/python-and-random-keys-of-21-char-max
            uuid_id = uuid.uuid4()
            access_token = uuid_id.bytes.encode("base64")[:21].replace('+','m')
            # Check that the access_token is not the same as the access token for other user
            user_aux = ExtendedUser.objects.filter(access_token=access_token)
            while len(user_aux) > 0:
                # Generate a new access_token
                uuid_id = uuid.uuid4()
                access_token = uuid_id.bytes.encode("base64")[:21]
                user_aux = ExtendedUser.objects.filter(access_token=access_token)
            # Associated the access token with the user
            user.access_token = access_token
            user.save()
            # Return access token
            response_data['status'] = 'correct login'
            response_data['access_token'] = access_token
            return HttpResponse(dumps(response_data), content_type="application/json")
        else:
            # Return error in the response
            response_data['status'] = 'incorrect password'
            # Return the JSON object
            return HttpResponse(dumps(response_data), content_type="application/json")

############################################################################################################################

def search_routes(request):
    # JSON objecto of the response
    response_data = {}

    ## Extract parameters from the request
    ######################################
    # From localization can be a string query or coordinates
    from_as_address = True
    if request.GET.has_key('from_address'):
        from_address = request.GET.get('from_address', None)
        from_address = from_address.encode('utf-8')
    elif request.GET.has_key('from_coord'):
        from_coord = request.GET.get('from_coord', None)
        from_as_address = False
    to_address = request.GET.get('to', None)
    to_address = to_address.encode('utf-8')
    # Time
    time = request.GET.get('time', None)
    # Time type
    time_type = request.GET.get('time_type', None)
    # Date
    date = request.GET.get('date', None)
    
    ######################################
    #     Request to Reittiopas API      #
    ######################################

    ## Geocoding of the origin and destination
    ############################################
    
    #### From
    if not from_as_address:
        # Retrieve the address of the location
        from_address = retrieve_address(from_coord)
        if from_address == 'not found':
            # Return error
            response_data['status'] = 'origin no localizable'
            # Return the JSON object
            return HttpResponse(dumps(response_data), content_type="application/json")

    # Retrieve the coordinates of the origin location
    parameters = {}
    # User
    parameters['user'] = reittiopas_api_user
    # Password
    parameters['pass'] = reittiopas_api_pass
    # Request type
    parameters['request'] = 'geocode'
    # From query
    from_address_aux = ""
    try:
        from_address_aux = from_address.encode('utf-8').replace(',', ' ').replace('.', ' ')
    except UnicodeDecodeError:
        from_address_aux = from_address.replace(',', ' ').replace('.', ' ')
    parameters['key'] = from_address_aux
    encode_parameters = urllib.urlencode(parameters)
    request_url = reittiopas_api_url+'?'+encode_parameters
    try:
        response = loads(urllib2.urlopen(request_url).read())
    except:
        # Return error
        response_data['status'] = 'origin no localizable'
        # Return the JSON object
        return HttpResponse(dumps(response_data), content_type="application/json")
    # extract coord
    from_coord = response[0].get('coords')
    from_address = response[0].get('name') 
        
    #### To
    # Retrieve the coordinates of the destination location
    parameters = {}
    # User
    parameters['user'] = reittiopas_api_user
    # Password
    parameters['pass'] = reittiopas_api_pass
    # Request type
    parameters['request'] = 'geocode'
    # From query
    to_address_aux = ""
    try:
        to_address_aux = to_address.encode('utf-8').replace(',', ' ').replace('.', ' ')
    except UnicodeDecodeError:
        to_address_aux = to_address.replace(',', ' ').replace('.', ' ')
    parameters['key'] = to_address_aux
    encode_parameters = urllib.urlencode(parameters)
    request_url = reittiopas_api_url+'?'+encode_parameters
    try:
        response = loads(urllib2.urlopen(request_url).read())
    except:
        # Return error
        response_data['status'] = 'destination no localizable'
        # Return the JSON object
        return HttpResponse(dumps(response_data), content_type="application/json")
    # extract coord
    to_coord = response[0].get('coords')
    to_address = response[0].get('name')

    ## Worked out the possible routes (Routing)
    ##############################################

    parameters = {}
    # User
    parameters['user'] = reittiopas_api_user
    # Password
    parameters['pass'] = reittiopas_api_pass
    # Request type
    parameters['request'] = 'route'
    # From coordinates
    parameters['from'] = from_coord
    # To coordinates
    parameters['to'] = to_coord
    # Time
    parameters['time'] = time
    # Time type
    parameters['timetype'] = time_type
    # date
    parameters['date'] = date
    encode_parameters = urllib.urlencode(parameters)
    request_url = reittiopas_api_url+'?'+encode_parameters
    response_routing = loads(urllib2.urlopen(request_url).read())

    #######################################
    #     Construct API response data     #
    #######################################

    # Fiter routes
    routes_list = filter_routes(response_routing, from_address, to_address)
    response_data['routes'] = routes_list
    response_data['status'] = 'OK'

    # Return the JSON object
    return HttpResponse(dumps(response_data), content_type="application/json")

############################################################################################################################

def save_journey(request):
    # Extract the access token from the request
    access_token = request.GET.get('access_token', "")
    print access_token

    # Extract the from, to and journey identifier
    from_address = request.GET.get('from', None)
    to_address = request.GET.get('to', None)
    journey_id = request.GET.get('id', None)

    response_data = {}
    # Retrieve the user associated with the access_token
    user = ExtendedUser.objects.filter(access_token=access_token)
    if len(user)==0:
        # Access token not valid
        response_data['status'] = 'access token is not valid'
        return HttpResponse(dumps(response_data), content_type="application/json")

    user = user[0]
    # Create a new saved trip associated with the user
    user.journey_set.create(journey_id=journey_id, origin=from_address, destination=to_address)

    response_data['status'] = 'journey saved'
    return HttpResponse(dumps(response_data), content_type="application/json")

############################################################################################################################

def retrieve_saved_journeys(request):
    # Extract the access token from the request
    access_token = request.GET.get('access_token', "")

    response_data = {}
    # Retrieve the user associated with the access_token
    user = ExtendedUser.objects.filter(access_token=access_token)
    if len(user)==0:
         # Access token not valid
        response_data['status'] = 'access token is not valid'
        return HttpResponse(dumps(response_data), content_type="application/json")
    
    user = user[0]
    # Retrieve all the saved trips for the user
    saved_journeys = user.journey_set.all()

    # Introduce the journey data in the response
    journey_list = []
    for journey in saved_journeys:
        journey_dict = {}
        journey_dict["id"] = journey.journey_id
        journey_dict["from"] = journey.origin
        journey_dict["to"] = journey.destination
        journey_list.append(journey_dict)
    response_data['journeys'] = journey_list

    response_data['status'] = 'journeys retrieved'
    return HttpResponse(dumps(response_data), content_type="application/json")