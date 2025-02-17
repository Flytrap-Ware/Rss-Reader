package com.flytrap.rssreader.global.presentation.resolver;

import com.flytrap.rssreader.api.auth.presentation.dto.AccountCredentials;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Slf4j
@Getter
@Component
@RequestScope
public class AuthorizationContext {

    private Optional<AccountCredentials> accountCredentials;

    public void setLoginAccount(AccountCredentials accountCredentials) {
        this.accountCredentials = Optional.ofNullable(accountCredentials);
    }
}
