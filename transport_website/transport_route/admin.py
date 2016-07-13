from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from django.contrib.auth.forms import UserChangeForm

# Import models
from transport_route.models import *

# Configure administration of trips
class JourneyInline(admin.StackedInline):
    model = Journey
    extra = 0

class ExtendedUserChangeForm(UserChangeForm):
    class Meta(UserChangeForm.Meta):
        model = ExtendedUser

class ExtendedUsersAdmin(UserAdmin):
    form = ExtendedUserChangeForm

    fieldsets = UserAdmin.fieldsets + (
        (None, {'fields': ('access_token',)}),
    )

    # Show the trips linked with a user
    inlines = [JourneyInline]

admin.site.register(ExtendedUser, ExtendedUsersAdmin)