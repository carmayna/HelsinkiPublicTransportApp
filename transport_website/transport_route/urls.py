# Python script to map an URL to a view

from django.conf.urls import patterns, url

from transport_route import views

urlpatterns = patterns('',
	# Index
    url(r'^$', views.index, name='index'),
    # Register user
    url(r'^register_user$', views.register_user, name='register_user'),
    # Login
    url(r'^login$', views.login, name='login'),
    # Search routes
    url(r'^search_routes$', views.search_routes, name='search_routes'),
    # Save a journey
    url(r'^save_journey$', views.save_journey, name='save_journey'),
    # Calculate route
    url(r'^retrieve_saved_journeys$', views.retrieve_saved_journeys, name='retrieve_saved_journeys'),
)