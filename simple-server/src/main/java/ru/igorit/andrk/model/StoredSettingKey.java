package ru.igorit.andrk.model;


import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class StoredSettingKey implements Serializable {
    @Column(name = "grp_name")
    public String group;
    @Column(name = "setting_name")
    public String setting;
}
