from json import *
import datetime
import urllib2, urllib
import parser
import codecs

# Reittiopas API
reittiopas_api_url = 'http://api.reittiopas.fi/hsl/prod/'
reittiopas_api_user = 'varnolen2'
reittiopas_api_pass = 'nafpass'

# Google geocoding api
google_api_key = 'AIzaSyDkocg3I73x_54BZjyNYyFhLiA_6AahbEE'
google_geocoding_url = 'https://maps.googleapis.com/maps/api/geocode/json'

############################################################
# Function to filter the response of API service "Routing" #
############################################################

def filter_routes(response_routing, from_address, to_address):
    # Maintain indexes of lines and stop names to speed the query
    line_name_index = {}

    routes_list = []

    # Reittitiopas response is an array with the different routes
    for i in xrange(len(response_routing)):
        route = response_routing[i][0]
        route_filtered = {}
        # Add the from and to localizations of the route
        route_filtered['from'] = from_address
        route_filtered['to'] = to_address
        # Read and convert the duration to string in minutes
        duration_sec = int(route['duration'])
        route_filtered['duration'] = duration_sec/60

        ################################
        # Filter the legs of the route #
        ################################
        legs_list = []
        for j in xrange(len(route['legs'])):
            leg = route['legs'][j]
            leg_aux = {}
            # Read and convert the duration to string in minutes
            duration_sec = int(leg['duration'])
            leg_aux['duration'] = duration_sec/60
            # Read type of the leg (walk, bus...)
            leg_aux['type'] = leg['type']
            ################# Walk leg ###################
            if leg_aux['type'] == 'walk':
                leg_aux['length'] = int(leg['length'])

                ###########################################
                # Filter the locations of the leg (stops) #
                ###########################################
                locs_list = []
                for z in xrange(len(leg['locs'])):
                    loc = leg['locs'][z]
                    loc_aux = {}
                    # Departure 
                    dept_time = loc['depTime']
                    loc_aux['depTime'] = dept_time[8:10]+":"+dept_time[10:]
                    # Name of the location
                    loc_aux['name'] = loc['name']
                    if loc.has_key('shortCode'):
                        loc_aux['shortCode'] = loc['shortCode'] # Code of the stop
                    # Append the loc to the list
                    locs_list.append(loc_aux)
                # If it is the same first leg set the name of the origin 
                if j == 0:
                    loc_aux = locs_list[0]
                    loc_aux['name'] = from_address.capitalize()
                    locs_list[0] = loc_aux
                if j == len(route['legs'])-1:
                    # If it is the last leg set the name of the destination
                    loc_aux = locs_list[len(locs_list)-1]
                    loc_aux['name'] = to_address.capitalize()
                    locs_list[len(locs_list)-1] = loc_aux

                # Filter locs (null and repeated values)
                locs_list_aux = []
                none_counter = 0
                for z in xrange(len(locs_list)):
                    loc = locs_list[z]
                    # Avoid locs with name equals to null
                    if loc['name']==None:
                        none_counter += 1
                        continue
                    else:
                        none_counter = 0
                    # Avoid locs that have with the same name as the previos
                    if (z>0) and (locs_list[z-(1+none_counter)]['name']==loc['name'] or locs_list_aux[len(locs_list_aux)-1]['name']==loc['name']):
                        continue
                    locs_list_aux.append(loc)
                locs_list = locs_list_aux
            else:
                ############# Other type of transport ###############
                # Line code
                if leg.has_key('code'):
                    code = leg['code']
                    # Get the short code of the line
                    if not line_name_index.has_key(code):
                        leg_aux['code'] = query_line_information(code, line_name_index)
                    else:
                        leg_aux['code'] = line_name_index[code]
                
                ###########################################
                # Filter the locations of the leg (stops) #
                ###########################################
                locs_list = []
                for loc in leg['locs']:
                    loc_aux = {}
                    # Departure time
                    dept_time = loc['depTime']
                    loc_aux['depTime'] = dept_time[8:10]+":"+dept_time[10:]
                    # Code of the stop
                    loc_aux['shortCode'] = loc['shortCode'] # Code of the stop
                    loc_aux['name'] = loc['name']
                    if loc_aux['shortCode'] == '':
                        loc_aux.pop('shortCode', None)
                    # Append the loc to the list
                    locs_list.append(loc_aux)
            leg_aux['locs'] = locs_list
            # Define the time of departure and arrival time for the leg (from the locs)
            leg_aux['depTime'] = locs_list[0]['depTime']
            leg_aux['arrTime'] = locs_list[len(locs_list)-1]['depTime']
            # Append the leg to the list
            legs_list.append(leg_aux)
        # Asign the legs to the route
        route_filtered['legs'] = legs_list

        # Define the time of departure and arrival time (from the legs)
        dept_time = legs_list[0]['depTime']
        route_filtered['depTime'] = legs_list[0]['depTime']
        route_filtered['arrTime'] = legs_list[len(legs_list)-1]['arrTime']

        # Append to the list of routes
        routes_list.append(route_filtered)
    return routes_list

######################################################################################

#####################################################################
# Function to retrieve the name of a line from the Reittitiopas API #
#####################################################################

def query_line_information(code, line_name_index):
    # Retrieve the short code of the line
    parameters = {}
    # User
    parameters['user'] = reittiopas_api_user
    # Password
    parameters['pass'] = reittiopas_api_pass
    # Request type
    parameters['request'] = 'lines'
    # Stop short code
    parameters['query'] = code
    encode_parameters = urllib.urlencode(parameters)
    request_url = reittiopas_api_url+'?'+encode_parameters
    response_line_info = loads(urllib2.urlopen(request_url).read())
    # Add the stop name to the index
    line_name_index[code] = response_line_info[0]['code_short']
    return response_line_info[0]['code_short']

######################################################################################

#####################################################################
# Retrieve address from coordinates usin Google Geocoding API       #
#####################################################################

def retrieve_address(coordinates):
    # Retrieve the short code of the line
    parameters = {}
    # API key
    parameters['key'] = google_api_key
    # Latitude and longitude
    parameters['latlng'] = coordinates
    # Device with location sensor
    parameters['sensor'] = 'true'
    # Result type
    parameters['result_type'] = 'street_address'
    encode_parameters = urllib.urlencode(parameters)
    request_url = google_geocoding_url+'?'+encode_parameters
    response_reverse_geocoding = loads(urllib2.urlopen(request_url).read())

    # Parse the response
    status = response_reverse_geocoding['status']
    if status == 'OK':
        results = response_reverse_geocoding['results']
        return results[0]['formatted_address']
    else:
        return 'not found'