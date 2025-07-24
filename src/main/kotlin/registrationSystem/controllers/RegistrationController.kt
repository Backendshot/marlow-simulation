package com.marlow.registrationSystem.controllers

import com.marlow.configuration.Config

class RegistrationController {
    val connection = Config().connect()
}