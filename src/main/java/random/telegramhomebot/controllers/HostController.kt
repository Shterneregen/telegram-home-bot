package random.telegramhomebot.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import random.telegramhomebot.AppConstants
import random.telegramhomebot.AppConstants.Hosts.ADD_EDIT_HOST_VIEW
import random.telegramhomebot.AppConstants.Hosts.DELETE_HOST_MAPPING
import random.telegramhomebot.AppConstants.Hosts.EDIT_HOST_BY_ID_MAPPING
import random.telegramhomebot.AppConstants.Hosts.EDIT_HOST_MAPPING
import random.telegramhomebot.AppConstants.Hosts.HOSTS_MAPPING
import random.telegramhomebot.AppConstants.Hosts.HOSTS_MODEL_ATTR
import random.telegramhomebot.AppConstants.Hosts.HOSTS_VIEW
import random.telegramhomebot.AppConstants.Hosts.HOST_ID_PATH_VAR
import random.telegramhomebot.AppConstants.Hosts.HOST_MAC_FIELD
import random.telegramhomebot.AppConstants.Hosts.HOST_MODEL_ATTR
import random.telegramhomebot.AppConstants.Hosts.REDIRECT_HOSTS
import random.telegramhomebot.AppConstants.Hosts.SAVE_HOST_MAPPING
import random.telegramhomebot.AppConstants.Redirects.ERROR_404_REDIRECT
import random.telegramhomebot.db.model.Host
import random.telegramhomebot.services.hosts.HostService
import random.telegramhomebot.utils.pagination.PagerHelper
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@Controller
@RequestMapping(HOSTS_MAPPING)
class HostController(private val hostService: HostService) {

    @Value("\${hosts.default.page.size}")
    private lateinit var defaultPageSize: Number

    @Value("\${hosts.default.sorting}")
    private lateinit var defaultSorting: String

    @Value("\${hosts.default.sorting.direction}")
    private lateinit var defaultSortingDirection: String

    @RequestMapping
    fun getAllHosts(
        @RequestParam("pageSize") pageSize: Int?,
        @RequestParam("page") currentPage: Int?,
        @RequestParam("sortBy") sortBy: String?,
        @RequestParam("direction") direction: String?,
        model: Model,
        request: HttpServletRequest?
    ): String {
        val pageSizeCookieName = "hostsPageSize"
        val pageable = PagerHelper.getPageable(
            pageSize ?: defaultPageSize.toInt(),
            currentPage ?: DEFAULT_CURRENT_PAGE,
            sortBy ?: defaultSorting,
            direction ?: defaultSortingDirection,
            pageSizeCookieName, request
        )
        val hosts = hostService.getAllHosts(pageable)
        model.addAttribute(HOSTS_MODEL_ATTR, hosts)
        PagerHelper.prepareModelForPager(
            model, hosts.totalPages, hosts.number, pageable.pageSize, pageSizeCookieName, HOSTS_MAPPING
        )
        return HOSTS_VIEW
    }

    @RequestMapping(path = [EDIT_HOST_MAPPING, EDIT_HOST_BY_ID_MAPPING])
    fun editHostById(model: Model, @PathVariable(HOST_ID_PATH_VAR) id: UUID?): String {
        val host =
            if (id == null) Host()
            else id.let { hostService.getHostById(it).orElse(null) } ?: return ERROR_404_REDIRECT

        model.addAttribute(HOST_MODEL_ATTR, host)
        return ADD_EDIT_HOST_VIEW
    }

    @RequestMapping(path = [DELETE_HOST_MAPPING])
    fun deleteHostById(@PathVariable(HOST_ID_PATH_VAR) id: UUID): String {
        hostService.deleteHostById(id)
        return REDIRECT_HOSTS
    }

    @PostMapping(path = [SAVE_HOST_MAPPING])
    fun createOrUpdateHost(@Valid host: Host, bindingResult: BindingResult): String {
        if (bindingResult.hasErrors()) {
            return ADD_EDIT_HOST_VIEW
        }
        val storedHost = hostService.getHostByMac(host.mac)
        val saveNewHostWithExistingMac = storedHost != null && host.id == null
        val editStoredHostMacToExisting = storedHost != null && storedHost.id != host.id
        if (saveNewHostWithExistingMac || editStoredHostMacToExisting) {
            bindingResult.rejectValue(HOST_MAC_FIELD, AppConstants.Messages.HOST_MAC_NOT_UNIQUE_MSG)
            return ADD_EDIT_HOST_VIEW
        }
        hostService.saveHost(host)
        return REDIRECT_HOSTS
    }

    companion object {
        private const val DEFAULT_CURRENT_PAGE = 1
    }
}
