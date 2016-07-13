from django.conf.urls import patterns, include, url
from django.conf.urls.static import static

from django.contrib import admin
admin.autodiscover()

urlpatterns = patterns('',
    # Admin site
    url(r'^admin/', include(admin.site.urls)),
    # Tranport route REST service
    url(r'^transport_route/', include('transport_route.urls', namespace="transport_route")),
)
