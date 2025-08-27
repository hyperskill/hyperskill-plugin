package org.hyperskill.academy.learning.configuration

import org.hyperskill.academy.learning.newproject.ui.errors.ErrorState

class CourseCantBeStartedException(val error: ErrorState) : Exception()