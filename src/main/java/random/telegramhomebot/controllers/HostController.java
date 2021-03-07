package random.telegramhomebot.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.services.HostService;
import random.telegramhomebot.utils.pagination.PagerHelper;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;
import java.util.UUID;

import static random.telegramhomebot.AppConstants.Hosts.ADD_EDIT_HOST_VIEW;
import static random.telegramhomebot.AppConstants.Hosts.DELETE_HOST_MAPPING;
import static random.telegramhomebot.AppConstants.Hosts.EDIT_HOST_BY_ID_MAPPING;
import static random.telegramhomebot.AppConstants.Hosts.EDIT_HOST_MAPPING;
import static random.telegramhomebot.AppConstants.Hosts.HOSTS_MAPPING;
import static random.telegramhomebot.AppConstants.Hosts.HOSTS_MODEL_ATTR;
import static random.telegramhomebot.AppConstants.Hosts.HOSTS_VIEW;
import static random.telegramhomebot.AppConstants.Hosts.HOST_ID_PATH_VAR;
import static random.telegramhomebot.AppConstants.Hosts.HOST_MAC_FIELD;
import static random.telegramhomebot.AppConstants.Hosts.HOST_MODEL_ATTR;
import static random.telegramhomebot.AppConstants.Hosts.REDIRECT_HOSTS;
import static random.telegramhomebot.AppConstants.Hosts.SAVE_HOST_MAPPING;
import static random.telegramhomebot.AppConstants.Messages.HOST_MAC_NOT_UNIQUE_MSG;

@RequiredArgsConstructor
@Controller
@RequestMapping(HOSTS_MAPPING)
public class HostController {

	private static final int DEFAULT_CURRENT_PAGE = 0;

	private final HostService hostService;

	@Value("${hosts.default.page.size}")
	private int defaultPageSize;
	@Value("${hosts.default.sorting}")
	private String defaultSorting;
	@Value("${hosts.default.sorting.direction}")
	private String defaultSortingDirection;

	@RequestMapping
	public String getAllHosts(@RequestParam("pageSize") Optional<Integer> pageSize,
	                          @RequestParam("page") Optional<Integer> currentPage,
	                          @RequestParam("sortBy") Optional<String> sortBy,
	                          @RequestParam("direction") Optional<String> direction,
	                          Model model, HttpServletRequest request) {
		String pageSizeCookieName = "hostsPageSize";
		PageRequest pageable = PagerHelper.getPageable(
				pageSize, defaultPageSize,
				currentPage, DEFAULT_CURRENT_PAGE, pageSizeCookieName,
				sortBy, defaultSorting,
				direction, defaultSortingDirection,
				request
		);

		Page<Host> hosts = hostService.getAllHosts(pageable);
		model.addAttribute(HOSTS_MODEL_ATTR, hosts);

		PagerHelper.prepareModelForPager(model, hosts.getTotalPages(),
				hosts.getNumber(), pageable.getPageSize(), pageSizeCookieName, HOSTS_MAPPING);
		return HOSTS_VIEW;
	}

	@RequestMapping(path = {EDIT_HOST_MAPPING, EDIT_HOST_BY_ID_MAPPING})
	public String editHostById(Model model, @PathVariable(HOST_ID_PATH_VAR) Optional<UUID> id) {
		model.addAttribute(HOST_MODEL_ATTR, Optional.ofNullable(id)
				.filter(Optional::isPresent)
				.flatMap(macOp -> hostService.getHostById(id.get()))
				.orElseGet(Host::new));
		return ADD_EDIT_HOST_VIEW;
	}

	@RequestMapping(path = DELETE_HOST_MAPPING)
	public String deleteHostById(@PathVariable(HOST_ID_PATH_VAR) UUID id) {
		hostService.deleteHostById(id);
		return REDIRECT_HOSTS;
	}

	@PostMapping(path = SAVE_HOST_MAPPING)
	public String createOrUpdateHost(@Valid Host host, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return ADD_EDIT_HOST_VIEW;
		}
		Optional<Host> storedHost = hostService.getHostByMac(host.getMac());
		boolean saveNewHostWithExistingMac = storedHost.isPresent() && host.getId() == null;
		boolean editStoredHostMacToExisting = storedHost.isPresent() && !storedHost.get().getId().equals(host.getId());
		if (saveNewHostWithExistingMac || editStoredHostMacToExisting) {
			bindingResult.rejectValue(HOST_MAC_FIELD, HOST_MAC_NOT_UNIQUE_MSG);
			return ADD_EDIT_HOST_VIEW;
		}
		hostService.saveHost(host);
		return REDIRECT_HOSTS;
	}
}
