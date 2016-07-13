#!/usr/bin/python
import sys, os
sys.path.insert(0, "/home/group36/group36/assignment_3/transport_website")

# Set the DJANGO_SETTINGS_MODULE environment variable.
os.environ['DJANGO_SETTINGS_MODULE'] = "transport_website.settings"
from django.core.servers.fastcgi import runfastcgi
runfastcgi(method="threaded", daemonize="false")