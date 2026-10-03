// 화면 문구 영어판. ko.ts 와 모양이 다르면 컴파일 실패

import type { Messages } from '@/shared/i18n/messages/ko'

const plural = (n: number, one: string, many: string) => (n === 1 ? one : many)

export const en: Messages = {
  app: {
    name: 'OdoLog',
    description: 'Keep your car’s service history and see when the next service is due.',
  },

  common: {
    save: 'Save',
    saving: 'Saving…',
    saved: 'Saved.',
    cancel: 'Cancel',
    edit: 'Edit',
    delete: 'Delete',
    deleting: 'Deleting…',
    register: 'Add',
    registering: 'Adding…',
    loading: 'Loading…',
    processing: 'Working…',
    optional: 'Optional',
    noChanges: 'Nothing changed.',
    previousPage: 'Previous page',
    nextPage: 'Next page',
    showAsTable: 'Show as table',
    showAsValues: 'Show values',
    none: 'None',
    email: 'Email',
    password: 'Password',
    nickname: 'Name',
    emailPlaceholder: 'you@example.com',
    count: (n: string) => n,
    records: (n: number) => `${n} ${plural(n, 'record', 'records')}`,
  },

  theme: {
    group: 'Appearance',
    light: 'Light',
    system: 'Match system',
    dark: 'Dark',
  },

  header: {
    login: 'Log in',
    logout: 'Log out',
  },

  footer: {
    privacy: 'Privacy',
    terms: 'Terms',
  },

  dateWheel: {
    change: 'Change',
    done: 'Done',
    year: '',
    month: '',
    day: '',
  },

  errors: {
    network: 'Couldn’t reach the server. Check your connection and try again.',
    http: (status: number) => `Request failed (HTTP ${status})`,
    codes: {
      VALIDATION_FAILED: (field: string) => `Please check ${field}.`,
      MALFORMED_BODY: (field: string) => `${field} isn’t in a valid format.`,
      INVALID_PARAMETER: (field: string) => `${field} isn’t valid.`,
      INVALID_SORT: 'That field can’t be sorted.',
      FUTURE_DATE: 'Dates after today aren’t allowed.',
      UNSUPPORTED_TIME_ZONE: 'That time zone isn’t supported.',
      UNSUPPORTED_CURRENCY: 'That currency isn’t supported.',
      SAME_PASSWORD: 'Your new password is the same as the current one.',
      BAD_REQUEST: 'Something about that request wasn’t right.',
      LOGIN_REQUIRED: 'Please log in.',
      LOGIN_FAILED: 'Wrong email or password.',
      WRONG_PASSWORD: 'Your current password is incorrect.',
      RESET_LINK_INVALID: 'This link has expired or was already used. Please request a new one.',
      FORBIDDEN: 'You don’t have access to that.',
      CSRF_REJECTED: 'We couldn’t verify this request. Refresh the page and try again.',
      VEHICLE_NOT_FOUND: 'Vehicle not found.',
      MAINTENANCE_RECORD_NOT_FOUND: 'Service record not found.',
      FUEL_RECORD_NOT_FOUND: 'Fuel record not found.',
      NOT_FOUND: 'Page not found.',
      METHOD_NOT_ALLOWED: 'That request method isn’t allowed.',
      UNSUPPORTED_MEDIA_TYPE: 'That request format isn’t supported.',
      PAYLOAD_TOO_LARGE: 'That’s too large to send. Check the file size.',
      EMAIL_DUPLICATE: 'An account with this email already exists.',
      PLATE_DUPLICATE: 'You’ve already added a vehicle with this plate.',
      ODOMETER_DECREASE: 'The odometer can’t go down.',
      DUPLICATE_VALUE: 'That value already exists. Refresh and try again.',
      CONCURRENT_UPDATE: 'This was changed somewhere else first. Refresh and try again.',
      TOO_MANY_LOGIN_ATTEMPTS: (minutes: number) =>
        `Too many login attempts. Try again in ${minutes} ${plural(minutes, 'minute', 'minutes')}.`,
      TOO_MANY_SIGNUP_ATTEMPTS: (minutes: number) =>
        `Too many sign-up attempts. Try again in ${minutes} ${plural(minutes, 'minute', 'minutes')}.`,
      TOO_MANY_RESET_REQUESTS: (minutes: number) =>
        `Too many reset requests. Try again in ${minutes} ${plural(minutes, 'minute', 'minutes')}.`,
      TOO_MANY_PASSWORD_ATTEMPTS: (minutes: number) =>
        `Too many wrong passwords. Try again in ${minutes} ${plural(minutes, 'minute', 'minutes')}.`,
      SAVE_FAILED: 'Couldn’t save. Please try again shortly.',
      SERVER_ERROR: 'Something went wrong on our end. Please try again shortly.',
    },
    fields: {
      email: 'the email',
      password: 'the password',
      newPassword: 'the new password',
      currentPassword: 'the current password',
      nickname: 'the name',
      plateNumber: 'the plate',
      manufacturer: 'the make',
      modelName: 'the model',
      modelYear: 'the year',
      odometer: 'the odometer',
      serviceOdometer: 'the odometer',
      serviceDate: 'the service date',
      fueledAt: 'the fill-up date',
      liters: 'the fuel amount',
      totalCost: 'the amount paid',
      cost: 'the cost',
      memo: 'the note',
      description: 'the note',
      type: 'the service type',
      intervalKm: 'the distance interval',
      intervalMonths: 'the time interval',
      timeZone: 'the time zone',
      currency: 'the currency',
      language: 'the language',
      unitSystem: 'the units',
    },
  },

  password: {
    hint: 'At least 8 characters',
    tooLong: (limit: number, bytes: number) =>
      `Over ${limit} bytes (currently ${bytes}). Some letters count as more than one byte.`,
    mismatch: 'The new passwords don’t match.',
    newPassword: 'New password',
    confirm: 'Confirm new password',
    current: 'Current password',
  },

  login: {
    title: 'Log in',
    description: 'Pick up where you left off with your vehicles.',
    submit: 'Log in',
    submitting: 'Logging in…',
    failed: 'Couldn’t log in.',
    noAccount: 'Don’t have an account?',
    signUp: 'Sign up',
    forgot: 'Forgot your password?',
    notices: {
      signedUp: 'Your account is ready. Please log in.',
      passwordReset: 'Password changed. Log in with your new password.',
    },
  },

  signUp: {
    title: 'Sign up',
    description: 'All you need is one car.',
    submit: 'Create account',
    submitting: 'Creating…',
    failed: 'Couldn’t create your account.',
    haveAccount: 'Already have an account?',
    login: 'Log in',
    agreement: (terms: string, privacy: string) => `By signing up you agree to the ${terms} and ${privacy}.`,
  },

  forgotPassword: {
    title: 'Reset password',
    description: 'We’ll email a reset link to the address you signed up with.',
    hint: 'We’ll send the link to the address you signed up with.',
    submit: 'Send reset link',
    submitting: 'Sending…',
    failed: 'Request failed.',
    sent: 'If that address has an account, we’ve sent a reset link. Check your inbox.',
    sentDetail:
      'The link works for 30 minutes and only once. If nothing arrives, check your spam folder or try again in a little while.',
    backToLogin: 'Back to log in',
  },

  resetPassword: {
    title: 'New password',
    description: 'Use at least 8 characters. You’ll log in again afterwards.',
    noTokenDescription: 'Open this page from the link in your email.',
    invalidLink: 'This reset link isn’t valid.',
    invalidLinkDetail: 'Open the link exactly as it appears in the email. If it expired, you can request a new one.',
    requestAgain: 'Get a new reset link',
    submit: 'Change password',
    submitting: 'Changing…',
    failed: 'Couldn’t reset your password.',
  },

  profile: {
    title: 'My account',
    account: {
      title: 'Account',
      description: 'You can change your name. Your email can’t be changed.',
      failed: 'Couldn’t save.',
    },
    password: {
      title: 'Password',
      description: 'Enter your current password to set a new one. You’ll stay logged in.',
      submit: 'Change password',
      submitting: 'Changing…',
      changed: 'Password changed.',
      failed: 'Couldn’t change your password.',
    },
    region: {
      title: 'Language & units',
      description:
        'Choose the language, distance and fuel units, currency, and time zone. “Today” and due dates follow this time zone.',
      language: 'Language',
      unitSystem: 'Units',
      currency: 'Currency',
      timeZone: 'Time zone',
      currencyHint: 'Existing records keep their original currency. Totals only include records in the current one.',
      timeZoneHint: (browser: string) => `This device’s time zone: ${browser}`,
      failed: 'Couldn’t save your settings.',
    },
    appearance: {
      title: 'Appearance',
      description: 'Pick light or dark, or follow your device.',
      mode: 'Theme',
      followsSystem: (dark: boolean) => `Following your device. Currently ${dark ? 'dark' : 'light'}.`,
      fixed: (dark: boolean) => `Always ${dark ? 'dark' : 'light'}.`,
    },
    data: {
      title: 'My data',
      description: 'Download your vehicles, service history, and fuel records as one JSON file. Passwords aren’t included.',
      exportNote: 'Deleted accounts can’t be restored. Download a copy first.',
      export: 'Download JSON',
      exporting: 'Preparing…',
      exportFailed: 'Couldn’t export your data.',
      importNote: 'Bring a downloaded file back in. Duplicate records are skipped, so importing twice is safe.',
      import: 'Import JSON',
      importing: 'Importing…',
      importFailed: 'Couldn’t import that file.',
      notOurFile: 'Please check that this is a JSON file downloaded from OdoLog.',
      imported: (vehicles: number, records: number) =>
        `Added ${vehicles} ${plural(vehicles, 'vehicle', 'vehicles')} and ${records} ${plural(records, 'record', 'records')}.`,
      merged: (n: number) => ` Attached records to ${n} existing ${plural(n, 'vehicle', 'vehicles')}.`,
      intervals: (n: number) => ` Restored ${n} custom service ${plural(n, 'interval', 'intervals')}.`,
      skipped: (n: number) => ` Skipped ${n} ${plural(n, 'record', 'records')} you already had.`,
    },
    withdraw: {
      title: 'Delete account',
      description: 'Deletes your account, vehicles, service history, and fuel records. This can’t be undone.',
      note: 'You can sign up again with the same email, but your records won’t come back.',
      open: 'Delete account',
      passwordHint: 'Enter your current password to confirm it’s you.',
      submit: 'Delete my account',
      submitting: 'Deleting…',
      failed: 'Couldn’t delete your account.',
    },
  },

  units: {
    KM_PER_L: 'km · L · km/L',
    L_PER_100KM: 'km · L · L/100km',
    MPG_US: 'miles · US gallons · mpg',
    MPG_UK: 'miles · litres · UK mpg',
  },

  languages: {
    KO: '한국어',
    EN: 'English',
  },

  vehicles: {
    myVehicles: 'My vehicles',
    modelYear: (year: number) => `${year}`,
    unknownYear: 'Year unknown',
    overdue: (n: number) => `${n} ${plural(n, 'service', 'services')} overdue`,
    dueSoon: (n: number) => `${n} due soon`,
    list: {
      title: 'My vehicles',
      count: (n: number) => `Tracking ${n} ${plural(n, 'vehicle', 'vehicles')}.`,
      register: 'Add vehicle',
      loadFailed: 'Couldn’t load your vehicles.',
      emptyTitle: 'No vehicles yet',
      emptyBody: 'Add the car you drive now, as it is. Enter the current odometer and a few services you remember, and the next service shows up right away.',
      registerFirst: 'Add your first vehicle',
    },
    form: {
      sectionTitle: 'Vehicle details',
      sectionDescription:
        'A plate only needs to be unique among your own vehicles. It doesn’t matter if someone else registered the same one.',
      plateNumber: 'License plate',
      plate: 'Plate',
      manufacturer: 'Make',
      modelName: 'Model',
      model: 'Model',
      modelYear: 'Year',
      platePlaceholder: 'ABC 1234',
      manufacturerPlaceholder: 'Toyota',
      modelPlaceholder: 'Corolla',
      title: 'Add vehicle',
      failed: 'Couldn’t add the vehicle.',
      odometer: (unit: string) => `Current odometer (${unit})`,
      odometerHint: 'Exactly what the dashboard shows. A car you already drive doesn’t need to start at zero.',
    },
    gettingStarted: {
      title: 'Getting started',
      description:
        'You can start tracking a car you already drive today. Follow these steps and the next service and fuel economy fill in on their own.',
      progress: (done: number, total: number) => `${done} of ${total} steps done`,
      hide: 'Hide guide',
      done: 'Done',
      steps: {
        odometer: {
          title: 'Enter the current odometer',
          body: 'Exactly what the dashboard shows. Next service and fuel economy are measured from it.',
          action: 'Go to odometer',
        },
        services: {
          title: 'Add the services you remember',
          body: 'Knowing roughly when is enough. You can skip the cost and the mileage at the time.',
          action: 'Add',
        },
        fuel: {
          title: 'Log your next fill-up',
          body: 'When you fill up, note the odometer and the amount. The first fill-up becomes the baseline for fuel economy.',
          action: 'Go to fill-ups',
        },
        efficiency: {
          title: 'First fuel economy on the second fill-up',
          body: 'Log a second fill-up and the economy for the stretch in between appears. After that it builds with every fill-up.',
        },
      },
    },
    detail: {
      loadFailed: 'Couldn’t load this vehicle.',
      notFound: 'Vehicle not found.',
      deleteConfirm: 'This deletes the vehicle along with its service history and fuel records. Continue?',
      deleteNote: 'Deleting also removes its service history and fuel records.',
      delete: 'Delete vehicle',
      deleteFailed: 'Couldn’t delete.',
      odometerAnnounce: (value: string) => `Odometer ${value}`,
    },
    odometer: {
      title: 'Update odometer',
      label: (unit: string) => `Current odometer (${unit})`,
      submit: 'Update',
      failed: 'Couldn’t update the odometer.',
      bigJump: (from: string, to: string) =>
        `That’s a big jump from ${from} to ${to}.\nPlease check the digits.\n\nSave anyway?`,
      decrease: (current: string) =>
        `That’s lower than the recorded ${current}.\nOnly continue if the odometer was replaced or you’re fixing a typo.`,
      conflict: (message: string, current: string) => `${message} (currently ${current})`,
    },
    info: {
      title: 'Vehicle details',
      unknown: 'Unknown',
      year: (year: number) => `${year}`,
      yearHint: 'Leave blank to keep it as is',
      failed: 'Couldn’t update the vehicle.',
    },
  },

  odometerHints: {
    past: (base: string) => `Lower than the recorded ${base}. Leave it if this is an older record.`,
    bigJump: (base: string) => `A big jump from the recorded ${base}. Please check the digits.`,
    bigJumpConfirm: (from: string, to: string) =>
      `The odometer jumps from ${from} to ${to}.\nIf a digit is wrong, the vehicle’s odometer gets stuck at that value.\n\nSave anyway?`,
  },

  serviceTypes: {
    ENGINE_OIL: 'Engine oil',
    TRANSMISSION_FLUID: 'Transmission fluid',
    SPARK_PLUG: 'Spark plugs',
    TIMING_BELT: 'Timing belt',
    COOLANT: 'Coolant',
    BRAKE_PAD: 'Brake pads',
    BRAKE_FLUID: 'Brake fluid',
    TIRE: 'Tires',
    TIRE_ROTATION: 'Tire rotation',
    WHEEL_ALIGNMENT: 'Wheel alignment',
    AIR_FILTER: 'Air filter',
    CABIN_FILTER: 'Cabin filter',
    BATTERY: 'Battery',
    WIPER: 'Wipers',
    OTHER: 'Other',
  },

  serviceTypeGroups: {
    engine: 'Engine & drivetrain',
    brakes: 'Brakes',
    tires: 'Tires & steering',
    consumables: 'Filters & parts',
    other: 'Other',
  },

  maintenance: {
    title: 'Service history',
    filterLabel: 'Filter by service type',
    allTypes: 'All types',
    add: 'Add service',
    loadFailed: 'Couldn’t load service history.',
    empty: 'No service records yet.',
    emptyFiltered: (type: string) => `No ${type} records. Choose “All types” above to see everything.`,
    deleteConfirm: 'Delete this service record?',
    deleteFailed: 'Couldn’t delete.',
    form: {
      type: 'Service type',
      date: 'Service date',
      odometer: (unit: string) => `Odometer at service (${unit})`,
      odometerEmpty: 'Leave it blank and the next service is worked out from the date only.',
      cost: (currency: string) => `Cost (${currency})`,
      costEmpty: 'Leave it blank if you don’t know. It’s only left out of cost totals.',
      memo: 'Note',
      memoPlaceholder: 'Parts replaced, shop name, etc.',
      failed: 'Couldn’t save.',
    },
    quick: {
      intro: 'Pick when each was last done. Leave anything you don’t remember as it is.',
      unknown: 'Don’t know',
      monthsAgo: (months: number) =>
        months === 12 ? '1 year ago' : `${months} ${plural(months, 'month', 'months')} ago`,
      pickDate: 'Pick a date',
      odometer: (unit: string) => `Odometer then (${unit}, optional)`,
      submit: 'Save',
      nothingPicked: 'Nothing picked yet. Even one item you remember helps.',
      failed: 'Couldn’t save.',
      partlySaved: (saved: number, reason: string) =>
        `Saved ${saved}, but couldn’t save the rest. ${reason}`,
    },
    next: {
      title: 'Next service',
      description: 'Calculated from each type’s recommended interval and your last record. Overdue items come first, then those due soon (within 1,000 km or a month).',
      loadFailed: 'Couldn’t load upcoming services.',
      empty: 'Nothing to calculate yet. Add a service record and we’ll show when that type is due next.',
      overdue: 'Overdue',
      dueSoon: 'Due soon',
      interval: 'Interval',
      intervalCustomized: 'Custom interval',
      last: (parts: string) => `Last ${parts}`,
      or: ' or ',
      noInterval: 'No recommended interval',
      intervalDistance: (unit: string) => `Distance interval (${unit})`,
      intervalMonths: 'Time interval (months)',
      intervalEmptyHint: 'Leave blank to use the default (grey number).',
      intervalNote: 'Applies to this vehicle only. Clear both to go back to the defaults.',
      intervalFailed: 'Couldn’t save the interval.',
    },
  },

  fuel: {
    title: 'Fill-ups',
    add: 'Add fill-up',
    loadFailed: 'Couldn’t load fill-ups.',
    empty: 'No fill-ups yet. Fuel economy starts from your second one.',
    deleteConfirm:
      'Delete this fill-up?\n\nIts fuel amount goes with it, so the next record’s fuel economy will read higher than it really was.',
    deleteFailed: 'Couldn’t delete.',
    noLiters: 'No fuel amount',
    noEfficiency: '· no economy',
    resetPoint: 'Reset point',
    baseline: 'Baseline',
    fromNext: '· calculated from the next fill-up',
    suspicious: 'Check this',
    missing: 'Missing fill-up?',
    form: {
      date: 'Fill-up date',
      odometer: (unit: string) => `Odometer (${unit})`,
      odometerEmpty: 'Without an odometer reading, fuel economy can’t be calculated.',
      odometerHint: 'The number on your dash. If it’s higher than the vehicle’s odometer, the vehicle is updated too.',
      volume: (unit: string) => `Fuel amount (${unit})`,
      volumeEmpty: 'Leave blank and this stretch won’t get a fuel economy.',
      cost: (currency: string) => `Amount paid (${currency})`,
      costEmpty: 'Leave blank and it won’t count toward fuel spending.',
      pricePer: (price: string, unit: string) => `About ${price} per ${unit}`,
      memo: 'Note',
      memoPlaceholder: 'Station name, etc.',
      failed: 'Couldn’t save.',
      confirmIntro: 'If you save this:',
      confirmOutro: 'Continue?',
      warnSameOdometer: (current: string) =>
        `· The odometer matches the vehicle’s current value (${current}).\n  This stretch won’t get a fuel economy and the vehicle’s odometer won’t go up.`,
      warnNoVolume: '· The fuel amount is empty, so this stretch won’t get a fuel economy.',
      warnNoCost: '· The amount paid is empty, so it won’t count toward spending or unit price.',
      warnBigJump: (from: string, to: string) =>
        `· The odometer jumps from ${from} to ${to}.\n  If a digit is wrong, the vehicle’s odometer gets stuck at that value.`,
    },
    summary: {
      title: 'Fuel economy',
      loadFailed: 'Couldn’t load the fuel summary.',
      reset: 'Reset economy',
      unreset: 'Undo reset',
      resetConfirm: 'Leave out everything so far and start counting fuel economy again?',
      resetFailed: 'Couldn’t reset fuel economy.',
      afterReset: 'Fuel economy was reset. It’ll be calculated again from your next fill-up.',
      firstRecord: 'Your first fill-up is the baseline. Fuel economy starts from the next one.',
      noSegment:
        'No stretch to calculate yet. It takes two fill-ups in a row with a higher odometer and a fuel amount.',
      sinceReset: 'Only counts stretches since the reset.',
      excluded: (n: number) =>
        `Left out ${n} ${plural(n, 'stretch', 'stretches')} that can’t be right. Check the odometer or fuel amount on records marked “Check this”.`,
      missing: (n: number) =>
        `Left out ${n} ${plural(n, 'stretch', 'stretches')} that look like a missing fill-up. Add the missing record to include ${plural(n, 'it', 'them')} again.`,
      otherCurrency: (n: number, currency: string) =>
        `${n} ${plural(n, 'record', 'records')} not in ${currency} ${plural(n, 'is', 'are')} left out of total spending.`,
      records: 'Records',
      distance: 'Distance',
      volume: 'Fuel',
      cost: 'Spent',
      trend: (n: number) => `Last ${n} ${plural(n, 'stretch', 'stretches')}`,
      average: (value: string) => `avg ${value}`,
      axisNote: 'The axis starts near the lowest value, not zero. It shows change, not size.',
      lowerIsBetter: 'In these units, lower is better.',
    },
  },

  home: {
    title: (nickname: string) => `${nickname}’s garage`,
    description: 'Your vehicles, service and fuel records, and what you’ve spent — at a glance.',
    loadFailed: 'Couldn’t load your garage.',
    goRecord: 'Add a record',
    myVehicles: 'My vehicles',
    tiles: {
      vehicles: '',
      records: '',
      breakdown: (maintenance: string, fuel: string) => `Service ${maintenance} · Fuel ${fuel}`,
    },
    otherCurrency: (n: number, currency: string) =>
      `${n} ${plural(n, 'record', 'records')} not in ${currency} ${plural(n, 'is', 'are')} left out of totals and charts.`,
    vehicles: {
      title: 'By vehicle',
      maintenanceCount: (n: number) => `${n} ${plural(n, 'service', 'services')}`,
      noService: 'No services yet',
    },
    recent: {
      title: 'Recent activity',
      empty: 'No records yet.',
      fuel: 'Fill-up',
    },
    empty: {
      title: 'My garage',
      description: 'Add a vehicle and your stats will show up here.',
      heading: 'No vehicles yet',
      body: 'It doesn’t have to be a new car. Add the one you drive with its current odometer, and every fill-up and service you log builds up here.',
      registerFirst: 'Add your first vehicle',
    },
    monthly: {
      title: 'Last 12 months',
      description: 'Service and fuel spending per month.',
      axis: 'Month',
      month: 'Month',
      count: 'Records',
      maintenance: 'Service',
      fuel: 'Fuel',
      total: 'Total',
      tooltipCount: (month: string, n: number) => `${month} · ${n} ${plural(n, 'record', 'records')}`,
      breakdown: (maintenance: string, fuel: string) => `Service ${maintenance} · Fuel ${fuel}`,
    },
    byType: {
      title: 'Cost by service type',
      description: 'All-time totals. Fuel isn’t included.',
      empty: 'No service records yet.',
    },
  },

  landing: {
    headline1: 'When was the last service,',
    headline2: 'and when’s the next?',
    lede: 'Log your services and fill-ups. OdoLog works out when the next service is due and what mileage you’re getting.',
    start: 'Get started',
    login: 'Log in',
    whatItDoes: 'What OdoLog does',
    highlights: {
      maintenance: {
        title: 'Service history',
        body: 'Oil, transmission fluid, brakes, tires and more — 15 types, with cost and notes.',
      },
      next: {
        title: 'Next service',
        body: 'Worked out from each type’s recommended interval and your last record, by distance and by date.',
      },
      fuel: {
        title: 'Fill-ups & mileage',
        body: 'Note the odometer and how much you filled up, and fuel economy follows. Spending adds up too.',
      },
      odometer: {
        title: 'Odometer',
        body: 'Tracked per vehicle. A lower reading than before is only saved after you confirm it.',
      },
    },
    previewTitle: 'One car’s whole story on one screen',
    previewBody: 'Odometer, fuel economy, next service, and past records — no more hunting around.',
    previewPlate: 'ABC 1234',
    previewModel: 'Toyota Corolla',
    previewAverage: 'Average economy',
    closing: 'All you need is one car.',
    haveAccount: 'Already have an account?',
  },

  legal: {
    eyebrow: 'Legal',
    updated: (date: string) => `Last updated ${date}`,
    draftNotice:
      'This is a pre-launch draft. Fill in the operator details and contact, and have it reviewed before publishing.',
    privacy: {
      title: 'Privacy Policy',
      sections: [
        {
          heading: 'What we collect',
          body: [
            'Account: email, name, and password (stored only as a one-way hash).',
            'Settings: language, time zone, currency, and units.',
            'Records: vehicle details you enter (plate, make, model, year, odometer), service history, and fill-ups.',
            'We don’t collect phone numbers, addresses, or payment details.',
          ],
        },
        {
          heading: 'Why we use it',
          body: [
            'Only to log you in, keep your records, calculate next services and fuel economy, and send password reset emails.',
            'We don’t use it for ads or profiling, and we don’t sell it.',
          ],
        },
        {
          heading: 'Cookies',
          body: [
            'We use two cookies: a session cookie to keep you logged in and one to block forged requests. Both are strictly necessary. No tracking or advertising cookies.',
          ],
        },
        {
          heading: 'Who processes it for us',
          body: ['Password reset emails go out through an email delivery service, which receives the recipient’s address.'],
        },
        {
          heading: 'Retention and deletion',
          body: [
            'Deleting your account removes your account and all records immediately. This can’t be undone.',
            'Before deleting, you can download everything as a JSON file under “My account → My data”.',
          ],
        },
        {
          heading: 'Your rights',
          body: [
            'You can view, correct, download, and delete your data at any time. Most of this works right from “My account”; for anything else, contact us below.',
          ],
        },
        {
          heading: 'Contact',
          body: ['[Operator name] · [Contact email]'],
        },
      ],
    },
    terms: {
      title: 'Terms of Service',
      sections: [
        {
          heading: 'The service',
          body: ['OdoLog lets you record your vehicle’s service history and fill-ups, and shows the next service and fuel economy.'],
        },
        {
          heading: 'Calculated results',
          body: [
            'Next-service dates use general recommended intervals and are for reference only. Follow your vehicle’s manual and your mechanic.',
            'Fuel economy and totals come only from what you enter, so wrong inputs give wrong results.',
          ],
        },
        {
          heading: 'Your account',
          body: [
            'One account is for one person. Keep your password safe.',
            'We may limit access for use that interferes with other accounts or the service.',
          ],
        },
        {
          heading: 'Your records',
          body: ['What you enter belongs to you. You can download it or delete it by closing your account at any time.'],
        },
        {
          heading: 'Changes',
          body: ['If these terms change, we’ll note it on this page first.'],
        },
      ],
    },
  },
}
