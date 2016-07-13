from django.db import models
from django.contrib.auth.models import User, UserManager

###### CLASS User ######

class ExtendedUser(User):
    access_token = models.CharField(max_length=21, blank=True, null=True)

    # Use UserManager to get the create_user method, etc.
    objects = UserManager()

###### END OF CLASS User ######

###### CLASS Journey ######

class Journey(models.Model):
    user = models.ForeignKey(ExtendedUser)
    journey_id = models.CharField(max_length=100)
    origin = models.CharField(max_length=100)
    destination = models.CharField(max_length=100)
    
    # Use the journey id to represent a journey
    def __unicode__(self):
        return self.journey_id

###### END OF CLASS Journey ######