# Helsinki Public Transport app

###### Assignment of the course Network Application Framework
###### Course 2013/2014
###### University of Aalto, Espoo, Finland

### Introduction

In this course assignment I have developed a REST API and an Android App that lets users consult the public tranport route between two points of the Helsinki region.

### Rest API

The server REST API has been developed in Django. It consumes the data of the [Helsinki Region Transport HRT](http://www.programmableweb.com/api/helsinki-regional-transport-authority). Moreover, it offers a user management where a user can be registered in order to save favourite journeys.

In this way, the server preprocesses the data before it sends to the phone app, manage the requests to the HRT system and extends the functionality.

##### URLs

- Index of the REST API: {server_url}/transport_website/transporte_route/. Here you can find a detailed explanation of the services of the REST API
- Register a user: {server_url}/transport_website/transporte_route/register_user
- Login: {server_url}/transport_website/transporte_route/login
- Search route: {server_url}/transport_website/transporte_route/search_route
- Save journey: {server_url}/transport_website/transporte_route/save_journey
- Retrieve saved journeys: {server_url}/transport_website/transporte_route/retrieve_saved_journeys

##### Setup

- Download the  directory on the server from this repository.
- Create a new directory called transport_website in the folder public_html of the server.
- Copy the index.fcgi and .htaccess files from the assignment_3/transport_website folder in the new transport_website folder.
- Edit the index.fcgi file changing the next line: sys.path.insert(0, "{absolute_path_to_django_project}")
- Edit the file settings.py in social_webiste folder: STATIC_ROOT = '{absolute_path_to_static}'. This indicates the server where it have to find for the static files.
- Go to directory transport_website and type on the console: python manage.py collectstatic. This will copy all the static files in a new folder static. This is done to serve the static files in ther server-side.

### Android app

Android app that consumes the services of the developed API.

### Additional comments

The documentation of this project cane be found in this repository in the file *Assignment3_Documentation.pdf*.
