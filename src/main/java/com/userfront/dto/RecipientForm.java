package com.userfront.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.userfront.domain.Recipient;

public class RecipientForm {

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9 ()+-]{7,20}$", message = "Invalid phone number")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6,20}$", message = "Invalid account number")
    private String accountNumber;

    @Size(max = 255)
    private String description;

    public RecipientForm() {
    }

    public RecipientForm(Recipient recipient) {
        this.name = recipient.getName();
        this.email = recipient.getEmail();
        this.phone = recipient.getPhone();
        this.accountNumber = recipient.getAccountNumber();
        this.description = recipient.getDescription();
    }

    public void applyTo(Recipient recipient) {
        recipient.setName(name);
        recipient.setEmail(email);
        recipient.setPhone(phone);
        recipient.setAccountNumber(accountNumber);
        recipient.setDescription(description);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
